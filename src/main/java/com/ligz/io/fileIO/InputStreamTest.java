package com.ligz.io.fileIO;

import java.io.*;

/**
 * InputStream类
 * author:ligz
 */
public class InputStreamTest {
    public static void copyFile(String src, String dist) throws IOException {//实现文件复制
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dist);

        byte[] buffer = new byte[20 * 1024];
        int cnt;

        /**
         * read()最多读取 buffer.length 个字节
         * 返回的是实际读取的个数
         * 返回 -1 的时候表示读到 eof,即文件尾
         */
        while ((cnt = in.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, cnt);
        }

        in.close();
        out.close();
    }

    public static void buffer(String filePath) throws FileNotFoundException {
        //实例化一个具有缓存功能的字节流对象时，只需要在 FileInputStream 对象上再套一层 BufferedInputStream 对象即可
        //DataInputStream 装饰者提供了对更多数据类型进行输入的操作，比如 int、double 等基本类型
        FileInputStream fileInputStream = new FileInputStream(filePath);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
    }
}
