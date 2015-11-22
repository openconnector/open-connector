package com.connector.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connector.processor.TaskProcessor;

public class TaskProcessorDemo implements TaskProcessor{
	
	private static volatile long count=0;
	
	private Object lock = new Object();
	
	private Logger logger = LoggerFactory.getLogger(TaskProcessor.class);
	public byte[] process(byte[] content) {
		String str = null;
		try {
			synchronized (lock) {
				count++;
				str = "content is : " + new String(content) + "-" + count;
				logger.info(str);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return str.getBytes();
	}

}
