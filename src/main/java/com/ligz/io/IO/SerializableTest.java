package com.ligz.io.IO;

import java.io.*;

/**
 * 序列化就是将一个对象转换成字节序列，方便存储和传输序列化：
 * ObjectOutputStream.writeObject()
 * 反序列化：ObjectInputStream.readObject()
 * 不会对静态变量进行序列化，因为序列化只是保存对象的状态，静态变量属于类的状态
 *
 * author:ligz
 */
public class SerializableTest {

    private static class A implements Serializable {

        private int x;
        private String y;

        A(int x, String y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "x = " + x + "  " + "y = " + y;
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException{
        A a1 = new A(123, "abc");
        String objectFile = "file/a1";

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(objectFile));
        objectOutputStream.writeObject(a1);
        objectOutputStream.close();

        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(objectFile));
        A a2 = (A) objectInputStream.readObject();
        objectInputStream.close();
        System.out.println(a2);
    }
}
