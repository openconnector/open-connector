package com.connector.client;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.connector.client.async.ResponseCallback;
import com.connector.model.TransportModel;

/**
 * @date 2015-9-22
   @author Allen
 */
public class ClientEntry implements Delayed{
	private TransportModel requestModel;
	private TransportModel responseModel;
	private boolean isAsyn = false;
	private ResponseCallback responseCallback;
	private long time;
	
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public boolean isAsyn() {
		return isAsyn;
	}
	public void setAsyn(boolean isAsyn) {
		this.isAsyn = isAsyn;
	}

	public ResponseCallback getResponseCallback() {
		return responseCallback;
	}
	public void setResponseCallback(ResponseCallback responseCallback) {
		this.responseCallback = responseCallback;
	}
	public TransportModel getRequestModel() {
		return requestModel;
	}
	public void setRequestModel(TransportModel requestModel) {
		this.requestModel = requestModel;
	}
	public TransportModel getResponseModel() {
		return responseModel;
	}
	public void setResponseModel(TransportModel responseModel) {
		this.responseModel = responseModel;
	}
	public int compareTo(Delayed o) {
		ClientEntry that = (ClientEntry)o;
		if(this.time < that.time){
			return -1;
		}else if(this.time > that.time){
			return 1;
		}else{
			return 0;
		}
	}
	
	public long getDelay(TimeUnit unit) {
		return unit.convert(this.time - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof ClientEntry)){
			return false;
		}
		ClientEntry that = (ClientEntry)obj;
		TransportModel model = that.getRequestModel();
		if(model!=null && requestModel!=null){
			return requestModel.getTransparent() == model.getTransparent();
		}else{
			return super.equals(obj);	
		}
	}
	
};