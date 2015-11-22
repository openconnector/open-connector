package com.connector.client.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connector.client.ClientEntry;

/**
 * @date 2015-9-22
   @author Allen
 */
public class AsyncClientWorkers {
	
	private static Logger logger = LoggerFactory.getLogger(AsyncClientWorkers.class);
	private static AsyncClientRepository asynClientRepository = AsyncClientRepository.getInstance();
	
	public static final ExecutorService workers = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setName("AsynClientWorker");
			return thread;
		}
	});
	
	public static void submit(ClientEntry entry,boolean isSuccess){
		AsynClientWorker worker = new AsynClientWorker(entry, isSuccess);
		workers.submit(worker);
	}
	
	static class AsynClientWorker implements Runnable{
		
		private ClientEntry entry;
		private boolean isSuccess;
		
		public AsynClientWorker(ClientEntry entry,boolean isSuccess) {
			this.entry = entry;
			this.isSuccess = isSuccess;
		}
		
		public void run() {
			asynClientRepository.remove(entry);
			ResponseCallback future = entry.getResponseCallback();
			if(isSuccess){
				future.onSuccess(entry.getRequestModel().getContent(), entry.getResponseModel().getContent());
			}else{
				logger.error("request time out");
				future.onError(entry.getRequestModel().getContent());
			}
		}
	}
}
