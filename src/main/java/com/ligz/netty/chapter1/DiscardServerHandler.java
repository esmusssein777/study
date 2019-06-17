package com.ligz.netty.chapter1;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * 处理服务器
 * @author: ligz
 */
public class DiscardServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        /**
         * 将收到的消息回传
         * ctx.write(Object) 方法不会使消息写入到通道上，他被缓冲在了内部
         * 你需要调用 ctx.flush() 方法来把缓冲区中数据强行输出。
         * 或者你可以用更简洁的 cxt.writeAndFlush(msg) 以达到同样的目的
         */
        //ctx.write(msg);
        //ctx.flush();


        //接收到的消息在这里处理
        ByteBuf in= (ByteBuf) msg;
        try {
            while (in.isReadable()) {
                //将接收到的数据打印出来
                System.out.println((char) in.readByte());
                System.out.flush();
            }

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //当出现异常的时候关闭
        cause.printStackTrace();
        ctx.close();
    }
}
