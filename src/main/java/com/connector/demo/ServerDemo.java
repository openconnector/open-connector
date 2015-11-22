package com.connector.demo;

import com.connector.processor.TaskProcessor;
import com.connector.server.Server;

public class ServerDemo{
    
	public static void start() {
		TaskProcessor processor = new TaskProcessorDemo();
		Server server = Server.getInstance();
		server.start(processor);
//		server.close();
	}
	
	public static void main(String[] args) {
		start();
	}
}
