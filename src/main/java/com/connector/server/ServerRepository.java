package com.connector.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connector.contant.Constant;
import com.connector.model.TransportModel;
import com.connector.processor.TaskProcessor;
import com.connector.queue.ConcurrentBlockingQueue;
import com.connector.util.PropertiesUtils;

/**
 * @date 2015-9-11
   @author Allen
 */
public class ServerRepository {
	
	private static final Logger logger = LoggerFactory.getLogger(ServerRepository.class);
	private static int queneLength = PropertiesUtils.getInt("serverQueneLength", 2048);
	private static final int threadNum = PropertiesUtils.getInt("taskWorkerThreadNum", 20);
	private static final ConcurrentBlockingQueue<TransportModel> taskQueue = new ConcurrentBlockingQueue<TransportModel>(queneLength);
	
	private static volatile boolean isStop = false;
	private static TaskProcessor processor;
	private static List<Thread> threadList = new ArrayList<Thread>();

	static{
		init();
	}
	
	public static void init(){
		if(threadNum <= 0){
			logger.error("taskWorkerThreadNum must be greater than 0");
			return;
		}
		if(!isValidLength(queneLength)){
			logger.error("serverQueneLength must be greater than 0 and must be 2 of integer power");
			return;
		}
		for (int i = 0; i < threadNum; i++) {
			TaskWorker taskWorker = new TaskWorker(taskQueue);
			Thread taskWorkerThread = new Thread(taskWorker, "taskWorker-" + i);
			threadList.add(taskWorkerThread);
			taskWorkerThread.start();
		}
	}
	
	private static boolean isValidLength(int length){
		if(length <= 0){
			return false;
		}
		if((length & (length - 1)) == 0){
			return true;
		}
		return false;
	}
	
	public static void setTaskProcessor(TaskProcessor tp){
		processor = tp;
	}
	
	public static void addTask(TransportModel model){
		taskQueue.put(model);
	}
	
	private static class TaskWorker implements Runnable{
		private ConcurrentBlockingQueue<TransportModel> quene;
		
		public TaskWorker(ConcurrentBlockingQueue<TransportModel> quene){
			this.quene = quene;
		}
		
		public void run() {
			while(!isStop){
				runTask();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					logger.info("thread wakeup:" + e.getMessage());
				}
				logger.info(Thread.currentThread().getName() + " stop");
			}
		}
		
		private void runTask(){
			TransportModel model = null;
			try {
				while(!isStop && (model = quene.take()) != null){
					Channel channel = model.getChannel();
					if(channel == null){
						logger.error("TransportModel bind channel error");
						continue;
					}
					processTask(model,channel);
				}
			} catch (InterruptedException e) {
				logger.error("TaskWorker was interruput:" + e.getMessage());
			}
		}
		
		private void processTask(TransportModel model,Channel channel){
			if(Constant.HEARTBEAT_MESSAGE == model.getFlag()){
				processHeartBeat(model,channel);
			}else{
				processNormal(model,channel);
			}
		}
		
		private void processHeartBeat(TransportModel model,Channel channel){
			ChannelFuture future = channel.writeAndFlush(model);
			final SocketAddress address = channel.remoteAddress();
			future.addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.info("write heartbeat message complete,client-host:" + address);
				}
			});
		}

		private void processNormal(TransportModel model,Channel channel){
			byte[] ret = null;
			try{
				ret = processor.process(model.getContent());
			}catch(Exception e){
				logger.error("process method throws exception: " + e.getMessage());
				return;
			}
			TransportModel tm = new TransportModel();
			tm.setContent(ret);
			tm.setFlag(Constant.NORMAL_MESSAGE);
			tm.setTransparent(model.getTransparent());

			ChannelFuture future = channel.writeAndFlush(tm);
			final SocketAddress address = channel.remoteAddress();
			future.addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) throws Exception {
					logger.debug("normal message complete,host=" + address);
				}
			});
		}
	}
	
	public static void close(){
		isStop = true;
		if(threadList.size() == 0){
			return;
		}
		for (int i = 0; i < threadList.size(); i++) {
			Thread thread = threadList.get(i);
			thread.interrupt();
		}
	}
}
