package com.connector.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connector.hander.ClientTaskHandler;
import com.connector.hander.ConnectorDecoder;
import com.connector.hander.ConnectorEncoder;

public class TcpClient {
	
	private Logger logger = LoggerFactory.getLogger(TcpClient.class);

	private EventLoopGroup workGroup = null;
	private Bootstrap bootstrap = null;
	private ChannelFuture future = null;
	private final int clientIoWorkerNum = Runtime.getRuntime().availableProcessors();
	
	private static TcpClient instance = new TcpClient();

	public static TcpClient getInstance(){
		return instance;
	}
	
	private TcpClient(){
		workGroup = new NioEventLoopGroup(clientIoWorkerNum);
		bootstrap = new Bootstrap();
		bootstrap.group(workGroup).channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.handler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast(new ConnectorEncoder());
				pipeline.addLast(new ConnectorDecoder());
				pipeline.addLast(new ClientTaskHandler());
			}
		});
	}
	
	public Channel getChannel(String ip, int port){
		future = bootstrap.connect(ip,port);
		return future.channel();
	}
	
	public Channel getChannelSyn(String ip,int port){
		try {
			future = bootstrap.connect(ip, port).sync();
			return future.channel();
		} catch (InterruptedException e) {
			logger.error("getChannelSyn connect error");
		}
		return null;
	}
	
	public synchronized void closeWorkGroup(){
		workGroup.shutdownGracefully();
	}
}
