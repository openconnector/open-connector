package com.connector.client.api;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connector.client.ClientRepository;
import com.connector.client.ConnectionPool;
import com.connector.client.async.AsyncClientRepository;
import com.connector.client.async.ResponseCallback;
import com.connector.contant.Constant;
import com.connector.model.TransportModel;


/**
 * @date 2015-9-10
   @author Allen
 */
public class Client {
	
	private static Logger logger = LoggerFactory.getLogger(Client.class);
	private static AtomicLong count = new AtomicLong(0);
	private static ClientRepository container = ClientRepository.getInstance();
	private static AsyncClientRepository acontainer = AsyncClientRepository.getInstance();
	
	public static byte[] send(byte[] content,long timeout){
		return send(content, timeout,null);
	}
	
	
	public static byte[] send(byte[] content,long timeout,String group){
		if(StringUtils.isBlank(group)){
			group = Constant.DEFAULT_GROUP;
		}
		if(!ConnectionPool.isValidGroup(group)){
			logger.error("invalid group,please check your connector.properties");
			return null;
		}
		TransportModel request = new TransportModel();
		request.setContent(content);
		request.setFlag(Constant.NORMAL_MESSAGE);
		request.setTransparent(count.addAndGet(1));
		int status = container.addRequestModel(request, timeout,group);
		TransportModel response = container.getResponseModel(request);
		container.removeClientEntry(request);
		if(status == Constant.StatusCode.TIMEOUT){
			logger.info("read timeout!");
			return null;
		}
		if(status == Constant.StatusCode.ERROR){
			logger.info("internal error");
			return null;
		}
		if(response != null){
			return response.getContent();
		}
		return null;
	}
	
	public static boolean sendAsync(byte[] content,long timeout,ResponseCallback callback){
		return sendAsync(content,timeout,null,callback);
	}
	
	
	public static boolean sendAsync(byte[] content,long timeout,String group,ResponseCallback callback){
		if(StringUtils.isBlank(group)){
			group = Constant.DEFAULT_GROUP;
		}
		if(!ConnectionPool.isValidGroup(group)){
			logger.error("invalid group,please check your connector.properties");
			return false;
		}
		count.compareAndSet(Integer.MAX_VALUE, 0);
		TransportModel request = new TransportModel();
		request.setContent(content);
		request.setFlag(Constant.NORMAL_MESSAGE);
		request.setTransparent(count.addAndGet(1));
		long start = System.currentTimeMillis();
		boolean status = acontainer.addAsyncRequestModel(request,timeout,group,callback);
		if((System.currentTimeMillis() - start) > timeout){
			logger.error("write timeout");
			return false;
		}
		return status;
	}
	
	public static void close(){
		ConnectionPool.close();
		AsyncClientRepository.close();
	}
	
}
