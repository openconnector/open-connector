package com.connector.client;

import io.netty.channel.Channel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connector.contant.Constant;
import com.connector.model.TransportModel;
import com.connector.util.PropertiesUtils;

/**
 * @date 2015-9-10
   @author Allen
 */
public class ConnectionPool {
	
	private static Logger logger = LoggerFactory.getLogger(ConnectionPool.class);
	private static TcpClient tcpClient;
	private static Map<String, BlockingQueue<ChannelWrapper>> connectMap = new ConcurrentHashMap<String, BlockingQueue<ChannelWrapper>>();
	private static final int poolSize = PropertiesUtils.getInt("poolSize", 1);
	private static ScheduledExecutorService scheduledExecutorService;
	private static BlockingQueue<ChannelWrapper> failedQuene = new LinkedBlockingQueue<ChannelWrapper>();
	
	static{
		init();
	}
	
	private static void init(){
		Map<String, List<String>> hosts = PropertiesUtils.getListWithKeyPrefix("hosts-");
		int port = PropertiesUtils.getInt("port", 9999);
		if(hosts == null || hosts.size() == 0){
			logger.error("cannot find host ip config");
			return;
		}
		tcpClient = TcpClient.getInstance();
		initChannels(hosts,port);
		initChannelCheckAndRebuild();
	}
	
	public static boolean isValidGroup(String group){
		return connectMap.containsKey(group);
	}
	
	
	private static void initChannels(Map<String, List<String>> hosts,int port){
		for (Map.Entry<String, List<String>> entry : hosts.entrySet()) {
			BlockingQueue<ChannelWrapper> channelQuene = new LinkedBlockingQueue<ChannelWrapper>();
			List<String> ipList = entry.getValue();
			if(ipList == null || ipList.size() == 0){
				logger.error("group=" + entry.getKey() + " has not ip list");
				clearConnection();
				return;
			}
			String group = entry.getKey();
			for (String ip : ipList) {
				for (int i = 0; i < poolSize; i++) {
					Channel channel = tcpClient.getChannelSyn(ip,port);
					if(channel != null){
						channelQuene.add(new ChannelWrapper(ip, port, channel,group));
					}else{
						clearConnection();
						logger.error("get new connection error");
						return;
					}
				}
			}
			connectMap.put(entry.getKey(), channelQuene);
		}
	}
	
	private static void clearConnection(){
		connectMap.clear();
	}
	
	public static ChannelWrapper getChannel(String group,long timeout){
		BlockingQueue<ChannelWrapper> quene = connectMap.get(group);
		try {
			return quene.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			logger.error("getChannel interrupted");
		}
		return null;
	}
	
	public static void returnChannel(ChannelWrapper channelWrapper,String group){
		BlockingQueue<ChannelWrapper> quene = connectMap.get(group);
		if(quene == null){
			logger.error("group=" + group + " has no connection,return channel error");
			return;
		}
		quene.add(channelWrapper);
	}

	public static void close(){
		scheduledExecutorService.shutdownNow();
		closeChannels();
		tcpClient.closeWorkGroup();
		logger.info("client channels closed");
	}
	
	private static void closeChannels(){
		if(connectMap == null || connectMap.size() == 0){
			return;
		}
		for(Map.Entry<String, BlockingQueue<ChannelWrapper>> entry : connectMap.entrySet()){
			BlockingQueue<ChannelWrapper> queue = entry.getValue();
			ChannelWrapper channelWrapper = null;
			while((channelWrapper=queue.poll()) != null){
				channelWrapper.close();
			}
		}
	}
	
	
	private static void initChannelCheckAndRebuild(){
		scheduledExecutorService = Executors.newScheduledThreadPool(2);
		scheduledExecutorService.scheduleWithFixedDelay(new HeartBeatCheck(), 10, 10, TimeUnit.SECONDS);
		scheduledExecutorService.scheduleWithFixedDelay(new ChannelRebuild(), 10, 10, TimeUnit.SECONDS);
	}
	
	public static void addFailChannelWrapper(ChannelWrapper channelWrapper){
		failedQuene.add(channelWrapper);
	}
	
	static class HeartBeatCheck implements Runnable{
		public void run() {
			Thread.currentThread().setName("HeartBeatCheckThread");
			if(connectMap == null && connectMap.size() == 0){
				return;
			}
			for (Map.Entry<String, BlockingQueue<ChannelWrapper>> entry: connectMap.entrySet()) {
				BlockingQueue<ChannelWrapper> quene = entry.getValue();
				if(quene == null || quene.size() == 0){
					continue;
				}
				int totalChannel = quene.size();
				ChannelWrapper channelWrapper = null;
				int count = 0;
				try{
					while((channelWrapper=quene.poll()) != null){
						count++;
						if(!channelWrapper.isActive()){
							addFailChannelWrapper(channelWrapper);
							logger.error("channelWrapper is not active");
							continue;
						}
						TransportModel model = new TransportModel();
						model.setContent(null);
						model.setFlag(Constant.HEARTBEAT_MESSAGE);
						channelWrapper.writeAndFlush(model);
					    quene.add(channelWrapper);
					    if(count >= totalChannel){
							break;
						}
					}
				}catch(Exception e){
					logger.error("heartbeat check exception:");
				}
			}
		}
	}
	
	static class ChannelRebuild implements Runnable{
		public void run() {
			Thread.currentThread().setName("ChannelRebuildThread");
			ChannelWrapper channelWrapper = null;
			int total = failedQuene.size();
			int count = 0;
			if(total == 0){
				return;
			}
			try {
				while((channelWrapper = failedQuene.take()) != null){
					count++;
					Channel channel = channelWrapper.getChannel();
					if(channel != null && channel.isActive()){
						logger.info("not necessary rebuid channel,host=" + channelWrapper.getHost());
						returnChannel(channelWrapper, channelWrapper.getGroup());
						return;
					}
					channel = tcpClient.getChannel(channelWrapper.getHost(), channelWrapper.getPort());
					channelWrapper = new ChannelWrapper(channelWrapper.getHost(), channelWrapper.getPort(), channel, channelWrapper.getGroup());
					if(channelWrapper.isActive()){
						returnChannel(channelWrapper, channelWrapper.getGroup());
						logger.info("rebuild channel success,host=" + channelWrapper.getHost());
					}else{
						logger.info("enquene failedQuene,host=" + channelWrapper.getHost());
						failedQuene.add(channelWrapper);
					}
					if(count>=total){
						return;
					}
				}
			} catch (InterruptedException e) {
				logger.error("ChannelRebuild interrupt");
			}
		}
	}
}
