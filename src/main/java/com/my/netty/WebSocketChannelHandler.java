package com.my.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 连接时初始化各个组件
 */
public class WebSocketChannelHandler extends ChannelInitializer<SocketChannel> {
    protected void initChannel(SocketChannel socketChannel) throws Exception {


        socketChannel.pipeline().addLast("http-codec",new HttpServerCodec());
        socketChannel.pipeline().addLast("aggregator",new HttpObjectAggregator(65536));
        socketChannel.pipeline().addLast("http-chunked",new ChunkedWriteHandler());
        socketChannel.pipeline().addLast("handler",new WebSocketHandler());
    }
}
