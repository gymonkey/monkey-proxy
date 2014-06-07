package me.gymonkey.proxy.client;

import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socks.SocksInitRequestDecoder;
import io.netty.handler.codec.socks.SocksMessageEncoder;

public class ProxyClient {

    private static final Logger logger             = LoggerFactory.getLogger(ProxyClient.class);

    private static final String DEFAULT_LOCAL_ADDR = "0.0.0.0";
    private static final int    DEFAULT_LOCAL_PORT = 8086;

    ServerBootstrap             bootstrap;

    String                      localAddr;
    int                         localPort;

    public ProxyClient(String localAddr, int localPort){
        this.localAddr = Strings.isNullOrEmpty(localAddr) ? DEFAULT_LOCAL_ADDR : localAddr;
        this.localPort = localPort <= 0 ? DEFAULT_LOCAL_PORT : localPort;

        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1, new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("proxy-client-io-thread");
                return t;
            }
        });
        final ChannelHandler socksMsgEncoder = new SocksMessageEncoder();
        bootstrap = new ServerBootstrap();
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, (int) Short.MAX_VALUE);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, (int) Short.MAX_VALUE);
        bootstrap.childHandler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(socksMsgEncoder);
                ch.pipeline().addLast(new SocksInitRequestDecoder());
            }
        });
    }
    
    public void bindUninterruptibly(){
        bootstrap.bind(localAddr, localPort).awaitUninterruptibly();
    }
}
