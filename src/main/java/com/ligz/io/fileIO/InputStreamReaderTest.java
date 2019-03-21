package com.ligz.io.fileIO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * author:ligz
 */
public class InputStreamReaderTest {
    public static void readFileContent(String filePath) throws IOException {//实现逐行输出文本文件的内容
        FileReader fileReader = new FileReader(filePath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
        }

        //装饰者模式使得 BufferedReader 组合了一个Reader对象
        //在调用 BufferedReader 的 close() 方法时会调用 Reader 的 Close() 方法
        //因此只要一个 close()
        bufferedReader.close();
    }

    public static void stringBytes() throws UnsupportedEncodingException {
        //String 可以看成一个字符序列，可以指定一个编码方式将它编码为字节序列，也可以指定一个编码方式将一个字节序列解码为 String
        String str = "中文";
        byte[] bytes = str.getBytes("UTF-8");
        String str1 = new String(bytes, "UTF-8");
        System.out.println(str1);
    }
}
