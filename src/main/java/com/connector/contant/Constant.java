package com.connector.contant;

/**
 * @date 2015-9-9
   @author Allen
 */
public class Constant {
		
	public static final int NORMAL_MESSAGE = 0;			//normal message type
	public static final int HEARTBEAT_MESSAGE = 1;		//heartbeat message type
	
	public static final int INT_LEN = 4;				//int type length
	public static final int LONG_LEN = 8;				//long type length
	
	public static final int magic = 2008; 
	
	public static final String DEFAULT_GROUP = "hosts-default";
	
	public static class StatusCode{
		public static int SUCCESS = 0;
		public static int TIMEOUT = 1;
		public static int ERROR = 2;
	}
	
}
