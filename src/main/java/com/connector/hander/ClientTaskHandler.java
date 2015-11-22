package com.connector.hander;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connector.client.ClientRepository;
import com.connector.contant.Constant;
import com.connector.model.TransportModel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientTaskHandler extends ChannelInboundHandlerAdapter{
	
	private Logger logger = LoggerFactory.getLogger(ClientTaskHandler.class);
	
	private ClientRepository container = ClientRepository.getInstance();
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)throws Exception {
		TransportModel model = (TransportModel)msg;
		if(model.getFlag() == Constant.HEARTBEAT_MESSAGE){
			logger.info("heartbeat message,host=" + ctx.channel().remoteAddress());
			return;
		}
		container.addResponseModel(model);
	}
	
}
