package com.connector.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.connector.hander.ConnectorDecoder;
import com.connector.hander.ConnectorEncoder;
import com.connector.hander.ServerTaskHandler;
import com.connector.processor.TaskProcessor;
import com.connector.util.PropertiesUtils;

/**
 * @date 2015-9-8
   @author Allen
 */
public class Server {
	
	private Logger logger = LoggerFactory.getLogger(Server.class);
	
	private static final int serverPort = PropertiesUtils.getInt("port", 9999);
	private static final int ioWorkerNum = Runtime.getRuntime().availableProcessors();
	
	private static Server instance = new Server();
	private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	private EventLoopGroup workGroup = new NioEventLoopGroup(ioWorkerNum);
	
	private ChannelFuture mainFuture = null;
	private ServerBootstrap bootstrap = new ServerBootstrap();
	
	private volatile boolean started = false; 
	
	private Server(){}
	
	public static Server getInstance(){
		return instance;
	}
	
	private void innerStart(final TaskProcessor processor){
		ServerRepository.setTaskProcessor(processor);
		try{
			bootstrap.group(bossGroup, workGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new ChannelInitializer<Channel>() {
				@Override
				protected void initChannel(Channel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast(new ConnectorDecoder());
					pipeline.addLast(new ConnectorEncoder());
					pipeline.addLast(new ServerTaskHandler());
				}
			});
			started = true;
			mainFuture = bootstrap.bind(serverPort).sync();
			logger.info("Server is started");
			mainFuture.channel().closeFuture().sync();
		}catch(Exception e){
			logger.error(e.getMessage());
		}finally{
			workGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
			logger.info("Server is shutdown gracefully");
		}
	}
	
	public void close(){
		logger.info("Server","Server begin to shutdown...");
		if(mainFuture == null){
			logger.info("Server","mainFuture is null");
			return;
		}
		Channel mainChannel = mainFuture.channel();
		if(mainChannel == null){
			logger.info("Server","mainChannel is null");
			return;
		}
		mainChannel.close();
		bossGroup.shutdownGracefully();
		workGroup.shutdownGracefully();
		logger.info("Server mainChannel closed");
		ServerRepository.close();
	}
	
	public void start(final TaskProcessor processor){
		if(started){
			logger.info("Server has been stared");
			return;
		}
		Thread startThread = new Thread(new Runnable() {
			public void run() {
				instance.innerStart(processor);
			}
		},"server-start-thread");
		startThread.start();
	}
	
}
