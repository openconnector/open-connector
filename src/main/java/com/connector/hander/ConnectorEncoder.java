package com.connector.hander;

import com.connector.contant.Constant;
import com.connector.model.TransportModel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ConnectorEncoder extends MessageToByteEncoder<TransportModel> {
	
	@Override
	protected void encode(ChannelHandlerContext ctx, TransportModel msg,ByteBuf out) throws Exception {
		out.writeInt(Constant.magic);
		int totalLength = Constant.INT_LEN + Constant.LONG_LEN;
		boolean isEmpty = true;
		if(msg.getContent() != null && msg.getContent().length != 0){
			isEmpty = false;
		}
		if(!isEmpty){
			totalLength += msg.getContent().length;
		}
		out.writeInt(totalLength);
		out.writeInt(msg.getFlag());
		out.writeLong(msg.getTransparent());
		if(!isEmpty){
			out.writeBytes(msg.getContent());
		}
	}
}
