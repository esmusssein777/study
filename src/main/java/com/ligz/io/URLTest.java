package com.ligz.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * author:ligz
 */
public class URLTest {
    public static void main(String[] args) throws MalformedURLException, IOException {
        URL url = new URL("http://www.baidu.com");

        /* 字节流 */
        InputStream in = url.openStream();

        /* 字符流 */
        InputStreamReader ir = new InputStreamReader(in);

        /* 提供缓存的功能 */
        BufferedReader bf = new BufferedReader(ir);

        String line;
        while ((line = bf.readLine()) != null) {
            System.out.println(line);
        }

        bf.close();
    }
}
