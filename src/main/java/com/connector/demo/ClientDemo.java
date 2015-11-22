package com.connector.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connector.client.api.Client;
import com.connector.client.async.ResponseCallback;


public class ClientDemo {

   private static Logger logger = LoggerFactory.getLogger(ClientDemo.class);
	
  /**
   * This is the request-wait-respose
   */
   public static void request(){
	   long start = System.currentTimeMillis();
	   for (int i = 0; i < 100; i++) {
		   byte[] response = Client.send(("this is echo test").getBytes(), 3000);
		   logger.info(new String(response));
	   }
	   logger.info("cost time=" + (System.currentTimeMillis() - start));
	   //if you want close the client
	   Client.close();
   }
   
   
   /**
    * This is the asynchronous request
    */
   public static void asyncRequest(){
	   ResponseCallback callback = new ResponseCallback() {
			public void onSuccess(byte[] request, byte[] buf) {
				if(buf != null && buf.length > 0){
					String str = new String(buf);
					logger.info("This content is receive from server :" + str);
				}
			}
			public void onError(byte[] request) {
				String str = new String(request);
				logger.info("error:" + str);
			}
	   };
	   
	   long start = System.currentTimeMillis();
	   for (int i = 0; i < 10000; i++) {
		   Client.sendAsync(("this is echo test").getBytes(), 3000, callback);
	   }
	  logger.info("###################cost time=" + (System.currentTimeMillis() - start));
	}
   
	
   public static void main(String[] args) {
//	   request();
	   asyncRequest();
   }
   
   
}
