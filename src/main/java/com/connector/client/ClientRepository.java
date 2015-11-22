package com.connector.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connector.client.async.AsyncClientWorkers;
import com.connector.client.async.ResponseCallback;
import com.connector.contant.Constant;
import com.connector.model.TransportModel;

/**
 * @date 2015-9-10
   @author Allen
 */
public class ClientRepository {
	private Logger logger = LoggerFactory.getLogger(ClientRepository.class);
	
	private static ClientRepository instance = new ClientRepository();
	
	private ClientRepository(){}
	
	public static ClientRepository getInstance(){
		return instance;
	}
	
	private Map<Long, ClientEntry> containerMap = new ConcurrentHashMap<Long, ClientEntry>();
	
	/**
	 * @param model
	 * @param timeout
	 * @return 0:success,1:timeout, 2:error
	 */
	public int addRequestModel(TransportModel model,long timeout,String group){
		long start = System.currentTimeMillis();
		ClientEntry clientEntry = getClientEntry(model);
		clientEntry.setRequestModel(model);
		int status = writeRequest(clientEntry, timeout, start, group);
		if(status != Constant.StatusCode.SUCCESS){
			return status;
		}
		long leave = timeout - (System.currentTimeMillis() - start);
		if(leave <=0){
			logger.error("client write timeout more");
			return Constant.StatusCode.TIMEOUT;
		}
		synchronized (clientEntry) {
			try {
				if(clientEntry.getResponseModel() == null){//some server process task so fast
					clientEntry.wait(leave);
				}
			} catch (InterruptedException e) {
				logger.info("addRequestModel interrupt");
				return Constant.StatusCode.ERROR;
			}
		}
		if((System.currentTimeMillis()-start) >= timeout){
			logger.error("read timeout");
			return Constant.StatusCode.TIMEOUT;
		}
		return Constant.StatusCode.SUCCESS;
	}
	
	private int writeRequest(ClientEntry clientEntry,long timeout,long start,String group){
		ChannelWrapper channelWrapper = ConnectionPool.getChannel(group,timeout);
		if(channelWrapper == null){
			logger.error("group=" + group + " has no connection");
			return Constant.StatusCode.ERROR;
		}
		if((System.currentTimeMillis() - start) >= timeout){
			logger.error("get channel timeout");
			return Constant.StatusCode.TIMEOUT;
		}
		if(!channelWrapper.isActive()){
			ConnectionPool.addFailChannelWrapper(channelWrapper);
			logger.error("channel is not active");
			return Constant.StatusCode.ERROR;
		}
		channelWrapper.writeAndFlush(clientEntry.getRequestModel());
		ConnectionPool.returnChannel(channelWrapper,group);
		return Constant.StatusCode.SUCCESS;
	}
	
	public ClientEntry addRequestModelNotWait(TransportModel request,long timeout, String group,ResponseCallback callback){
		long start = System.currentTimeMillis();
		ClientEntry clientEntry = getClientEntry(request);
		clientEntry.setAsyn(true);
		clientEntry.setTime(System.currentTimeMillis() + timeout);
		clientEntry.setRequestModel(request);
		clientEntry.setResponseCallback(callback);
		int status = writeRequest(clientEntry, timeout, start, group);
		if(status != Constant.StatusCode.SUCCESS){
			return null;
		}
		return clientEntry;
	}
	
	
	public void addResponseModel(TransportModel model){
		ClientEntry clientEntry = getClientEntry(model);
		clientEntry.setResponseModel(model);
		if(clientEntry.isAsyn()){
			clientEntry = containerMap.remove(model.getTransparent());
			if(clientEntry == null){
				logger.error("time out id=" + model.getTransparent());
				return;
			}
			AsyncClientWorkers.submit(clientEntry, true);
		}else{
			synchronized (clientEntry) {
				clientEntry.notify();
			}
		}
	}
	
	public TransportModel getResponseModel(TransportModel model){
		ClientEntry clientEntry = getClientEntry(model);
		return clientEntry.getResponseModel();
	}
	
	private ClientEntry getClientEntry(TransportModel model){
		Long id = model.getTransparent();
		ClientEntry clientEntry = containerMap.get(id);
		if(clientEntry == null){
			clientEntry = new ClientEntry();
			containerMap.put(id, clientEntry);
		}
		return clientEntry;
	}
	
	public ClientEntry removeClientEntry(TransportModel model){
		if(model == null){
			return null;
		}
		Long id = model.getTransparent();
		return containerMap.remove(id);
	}
	
	public int getContainerSize(){
		return containerMap.size();
	}

}
