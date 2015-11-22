package com.connector.queue;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * @date 2015-10-10
   @author Allen
 */
public class ConcurrentBlockingQueue<E> {
	
	private AtomicLong takeIndex;
	private long p1,p2,p3,p4,p5,p6,p7;
	private AtomicLong putIndex;
	private long p8,p9,p10,p11,p12,p13,p14;
	private int capacity; //must be 2 integer power
	private Object[] ringBuffer;
	private int shouldNotifyCount;
	
	private final Object LOCK = new Object(); 
	
	public ConcurrentBlockingQueue(int size){
		ringBuffer = new Object[size];
		takeIndex = new AtomicLong(0);
		putIndex = new AtomicLong(0);
		capacity = size;
		shouldNotifyCount = (int)(capacity * 0.5);
	}
	
	public void put(E e){
		int retry = 10;
		boolean status = false;
		while(!status){
			long position = putIndex.longValue();
			if(position >= takeIndex.longValue() && position < (takeIndex.longValue() + capacity)){
				status = putIndex.compareAndSet(position, position+1);
				if(status){
					int index =(int)(position & (capacity-1));
					ringBuffer[index] = e;
					if((putIndex.longValue() - takeIndex.longValue()) >= shouldNotifyCount){
						synchronized(LOCK){
							LOCK.notifyAll();
						}
					}
					return;
				}
			}
			if(retry > 0){
				retry--;
			}else{
				LockSupport.parkNanos(1L);
			}
		}
	}
	
	public E take() throws InterruptedException{
		int retry = 10;
		int parkCount = 10;
		boolean status = false;
		while(!status){
			long position = takeIndex.longValue();
			if(position < putIndex.longValue()){
				status = takeIndex.compareAndSet(position, position+1);
				if(status){
					int index = (int)(position & (capacity-1));
					return (E)ringBuffer[index];
				}
			}
			if(retry > 0){
				retry--;
			}else if(parkCount > 0){
				LockSupport.parkNanos(1L);
				parkCount--;
			}else{
				synchronized (LOCK) {
					try {
						LOCK.wait(10);
					} catch (InterruptedException e) {
						throw e;
					}
				}
			}
		}
		return null;
	}
	
	public int size(){
		return (int)(putIndex.longValue() - takeIndex.longValue());
	}
	
	public boolean isEmpty(){
		return putIndex.longValue() == takeIndex.longValue();
	}
	
}
