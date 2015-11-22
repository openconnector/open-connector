package com.connector.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * @date 2015-9-14
   @author Allen
 */
public class ChannelWrapper {
	
	private String host;
	private int port;
	private Channel channel;
	private String group;
	
	public ChannelWrapper(String host,int port,Channel channel,String group){
		this.host = host;
		this.port = port;
		this.channel = channel;
		this.group = group;
	}
	
	public void close(){
		if(channel != null){
			channel.close();
		}
	}
	
	public ChannelFuture writeAndFlush(Object model){
		if(channel != null){
			return channel.writeAndFlush(model);
		}
		return null;
	}
	
	public boolean isActive(){
		if(channel == null){
			return false;
		}
		return channel.isActive();
	}
	
	
	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public Channel getChannel() {
		return channel;
	}
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
}
