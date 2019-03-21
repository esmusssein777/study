package com.ligz.io.NIO;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * author:ligz
 */
public class NIOTest {
    public static void fastCopy(String src, String dist) throws IOException {
        //获取源文件的输入字节流
        FileInputStream in = new FileInputStream(src);

        //获取输入字节流的文件通道
        FileChannel fcin = in.getChannel();

        //获取目标文件的输出字节流
        FileOutputStream out = new FileOutputStream(dist);

        //获取输出字节流的文件通道
        FileChannel fcout = out.getChannel();

        //为缓存区分配 1024 个字节
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

        while (true) {
            //从输入通道读取数据到缓存区
            int r = fcin.read(buffer);

            //读到末尾
            if (r == -1) {
                break;
            }

            //切换读写
            buffer.flip();

            //将缓存区内容写到输出文件中
            fcout.write(buffer);

            //清空缓存区
            buffer.clear();
        }
    }
}
