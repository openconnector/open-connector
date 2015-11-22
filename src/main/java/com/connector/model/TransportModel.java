package com.connector.model;

import io.netty.channel.Channel;

/**
 * @date 2015-9-8
   @author Allen
 */
public class TransportModel {
	
	private int flag;
	private long transparent;
	private byte[] content;
	private Channel channel;

	public Channel getChannel() {
		return channel;
	}
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	public int getFlag() {
		return flag;
	}
	public void setFlag(int flag) {
		this.flag = flag;
	}
	public long getTransparent() {
		return transparent;
	}
	public void setTransparent(long transparent) {
		this.transparent = transparent;
	}
	public byte[] getContent() {
		return content;
	}
	public void setContent(byte[] content) {
		this.content = content;
	}
}
