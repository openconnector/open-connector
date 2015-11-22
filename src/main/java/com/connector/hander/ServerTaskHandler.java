package com.connector.hander;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import com.connector.model.TransportModel;
import com.connector.server.ServerRepository;

public class ServerTaskHandler extends ChannelInboundHandlerAdapter{
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)throws Exception {
		if(msg == null){
			return;
		}
		TransportModel model = (TransportModel)msg;
		Channel channel = ctx.channel();
		model.setChannel(channel);
		ServerRepository.addTask(model);
	}
	
}
