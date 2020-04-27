package com.my.netty;

import com.my.netty.config.NettyConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.util.Date;

public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;

    private static final String WEB_SOCKET_URL = "ws://localhost:9988/websocket";

    /**
     * 服务器处理客户端核心方法
     * @param channelHandlerContext
     * @param msg
     * @throws Exception
     */
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {

        if(msg instanceof FullHttpMessage){
            //客户端向服务端发起http请求的业务
            handHttpRequest(channelHandlerContext,(FullHttpRequest)msg);
        }else if(msg instanceof WebSocketFrame){
            //处理websocket连接业务
            handWebsocketFrame(channelHandlerContext, (WebSocketFrame) msg);
        }
    }

    /**
     * 客户端与服务端创建连接
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.channelGroup.add(ctx.channel());
        System.out.println("客户端与服务端连接开启");
    }

    /**
     * 客户端与服务端断开连接
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyConfig.channelGroup.remove(ctx.channel());
        System.out.println("客户端与服务端断开连接");
    }

    /**
     * 服务端接收客户端发送过来的数据结束之后调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 异常调用
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 处理客户端向服务器端发起http握手请求的业务
     * @param ctx
     * @param req
     */
    private void handHttpRequest(ChannelHandlerContext ctx,FullHttpRequest req){
        if(!req.getDecoderResult().isSuccess() ||
            !("websocket".equals(req.headers().get("Upgrade")))){
            sendHttpResponse(ctx,req,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        WebSocketServerHandshakerFactory webSocketServerHandshakerFactory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL, null, false);
        handshaker = webSocketServerHandshakerFactory.newHandshaker(req);
        if(handshaker == null){
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        }else {
            handshaker.handshake(ctx.channel(),req);
        }
    }

    /**
     * 服务器端向客户端响应结果
     * @param ctx
     * @param req
     * @param res
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req,
                                  DefaultFullHttpResponse res){

        if(res.getStatus().code() != 200){
            ByteBuf byteBuf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(byteBuf);
            byteBuf.release();
        }

        //发送数据
        ChannelFuture channelFuture = ctx.channel().writeAndFlush(res);

        if(res.getStatus().code() != 200){
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * 处理客户端与服务器之间的websocket业务
     * @param ctx
     * @param frame
     */
    private void handWebsocketFrame(ChannelHandlerContext ctx,WebSocketFrame frame){

        //关闭
        if(frame instanceof CloseWebSocketFrame){
            handshaker.close(ctx.channel(),((CloseWebSocketFrame) frame).retain());
        }

        //ping
        if(frame instanceof PingWebSocketFrame){
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
        }

        //byte
        /*if(frame instanceof TextWebSocketFrame){
            System.out.println("暂不支持二进制消息");
            throw new RuntimeException("【"+this.getClass().getName()+"】不支持消息");
        }*/

        String text = ((TextWebSocketFrame) frame).text();
        System.out.println("服务端收到客户端的消息");

        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString() +
                                                                        ctx.channel().id() +
                                                                        "==========>>>>"
                                                                        + text);

        //群发消息
        NettyConfig.channelGroup.writeAndFlush(tws);


    }
}
