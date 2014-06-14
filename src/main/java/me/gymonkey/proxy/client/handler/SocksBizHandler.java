package me.gymonkey.proxy.client.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.handler.codec.socks.SocksAuthScheme;
import io.netty.handler.codec.socks.SocksCmdRequest;
import io.netty.handler.codec.socks.SocksCmdRequestDecoder;
import io.netty.handler.codec.socks.SocksCmdResponse;
import io.netty.handler.codec.socks.SocksCmdStatus;
import io.netty.handler.codec.socks.SocksInitRequest;
import io.netty.handler.codec.socks.SocksInitRequestDecoder;
import io.netty.handler.codec.socks.SocksInitResponse;
import io.netty.util.ReferenceCountUtil;

public class SocksBizHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SocksBizHandler.class);

    private static enum SocksStat {
        SOCKS_INIT, SOCKS_REQ, SOCKS_RESP, SOCKS_END
    }

    SocksStat checkpoint = SocksStat.SOCKS_INIT;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("connection has been closed");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        switch (checkpoint) {
            case SOCKS_INIT:
                SocksInitRequest initRequest = (SocksInitRequest) msg;

                // 只支持不需验证
                if (!initRequest.authSchemes().contains(SocksAuthScheme.NO_AUTH)) {
                    SocksInitResponse initResponse = new SocksInitResponse(SocksAuthScheme.UNKNOWN);
                    ctx.writeAndFlush(initResponse);
                    ctx.pipeline().addAfter("SOCKS_MSG_ENCODER", "SOCKS_REQUEST_DECODER", new SocksInitRequestDecoder());
                }

                SocksInitResponse initResponse = new SocksInitResponse(SocksAuthScheme.NO_AUTH);
                ctx.writeAndFlush(initResponse);
                ctx.pipeline().addAfter("SOCKS_MSG_ENCODER", "SOCKS_REQUEST_DECODER", new SocksCmdRequestDecoder());

                checkpoint = SocksStat.SOCKS_REQ;
                break;
            case SOCKS_REQ:
                SocksCmdRequest cmdRequest = (SocksCmdRequest) msg;

                logger.info(cmdRequest.cmdType().name());
                logger.info(cmdRequest.addressType().name());
                logger.info(cmdRequest.host());
                logger.info(String.valueOf(cmdRequest.port()));

                SocksCmdResponse cmdResponse = new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.IPv4,
                                                                    "127.0.0.1", 80);
                ctx.writeAndFlush(cmdResponse);

                checkpoint = SocksStat.SOCKS_RESP;
                break;
            default:
                ByteBuf buf = (ByteBuf) msg;
                byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                ReferenceCountUtil.release(msg);
                logger.info(new String(data));
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("InSocksBizHandler, Error Type: " + cause.getClass().getName() + ", Error Msg: "
                     + cause.getMessage());
        ctx.channel().close();
    }

}
