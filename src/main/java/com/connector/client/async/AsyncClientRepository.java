package com.connector.client.async;

import java.util.concurrent.DelayQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connector.client.ClientEntry;
import com.connector.client.ClientRepository;
import com.connector.model.TransportModel;


/**
 * @date 2015-9-22
   @author Allen
 */
public class AsyncClientRepository {

	private static Logger logger = LoggerFactory.getLogger(AsyncClientRepository.class);
	
	private static TimeoutScanThread scanThread = null;
	
	private static AsyncClientRepository instance = new AsyncClientRepository();
	
	private static DelayQueue<ClientEntry> delayQueue = new DelayQueue<ClientEntry>();
	
	private static ClientRepository clientRepository = ClientRepository.getInstance();
	
	
	
	private static boolean stop = false;
	
	private AsyncClientRepository() {
		if(scanThread == null){
			scanThread = new TimeoutScanThread();
		}
		scanThread.start();
	}
	
	public static AsyncClientRepository getInstance(){
		return instance;
	}

	public boolean add(ClientEntry entry){
		return delayQueue.add(entry);
	}
	
	public boolean remove(ClientEntry entry){
		return delayQueue.remove(entry);
	}
	
	public boolean addAsyncRequestModel(TransportModel request,long timeout,String group,ResponseCallback callback){
		ClientEntry entry = clientRepository.addRequestModelNotWait(request, timeout, group,callback);
		if(entry != null){
			delayQueue.add(entry);
			return true;
		}
		return false;
	}
	
	public static class TimeoutScanThread extends Thread{
		public void run(){
			super.setName("TimeoutScanThread");
			while(true && !stop){
				ClientEntry entry = null;
				try {
					while(!stop && (entry = delayQueue.take()) != null){
						entry = clientRepository.removeClientEntry(entry.getRequestModel());
						if(entry != null){
							AsyncClientWorkers.submit(entry, false);
						}else{
							AsyncClientWorkers.submit(entry, true);
						}
					}
				} catch (InterruptedException e) {
					logger.info("TimeoutScanThread interrupt");
				}finally{
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						logger.info("TimeoutScanThread while interrupt");
					}
				}
			}
		}
	}
	
	public static void close(){
		stop = true;
		scanThread.interrupt();
	}
	
}
