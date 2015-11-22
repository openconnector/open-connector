package com.connector.hander;

import java.util.List;

import com.connector.contant.Constant;
import com.connector.model.TransportModel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * message format : magic + totalLength + flag1 + flag2 + content
 * flag1: 0 nomal content, 1 heartbeat , flag2 : transparent transmission  
 * @date 2015-9-8
   @author Allen
 */
public class ConnectorDecoder extends ByteToMessageDecoder{
	
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,List<Object> out) throws Exception {
		if(in.readableBytes() < Constant.INT_LEN * 2){
			return;
		}
		int totalLength = 0;
		in.markReaderIndex();
		int magic = in.readInt();
		if(magic != Constant.magic){
			in.clear();
			return;
		}
		totalLength = in.readInt();
		if(in.readableBytes() < totalLength){
			in.resetReaderIndex();
			return;
		}
		int flag = in.readInt();
		long transparent = in.readLong();
		int contentLength = totalLength - Constant.INT_LEN - Constant.LONG_LEN;
		byte[] buf = null;
		if(contentLength > 0){
			buf = new byte[contentLength];
			in.readBytes(buf, 0, contentLength);
		}
		TransportModel model = new TransportModel();
		model.setContent(buf);
		model.setFlag(flag);
		model.setTransparent(transparent);
		out.add(model);
	}

}
