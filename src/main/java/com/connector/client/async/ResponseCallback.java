package com.connector.client.async;

/**
 * @date 2015-9-22
   @author Allen
 */
public interface ResponseCallback {
	
	/**
	 * invoke this method when success
	 * @param request
	 * @param response
	 */
	public void onSuccess(byte[] request,byte[] response);
	
	/**
	 * invoke this method when error or exception
	 * @param request
	 */
	public void onError(byte[] request);
	
	
}
