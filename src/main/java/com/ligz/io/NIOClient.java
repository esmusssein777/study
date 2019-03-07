package com.ligz.io;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * author:ligz
 */
public class NIOClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("127.0.0.1", 8888);
        OutputStream out = socket.getOutputStream();
        String s = "hello world";
        out.write(s.getBytes());
        out.close();
    }
}
