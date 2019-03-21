package com.ligz.io.fileIO;

import java.io.File;

/**
 * File 类可以用于表示文件和目录的信息，但是它不表示文件的内容。
 * 递归地列出一个目录下所有文件
 * author:ligz
 */
public class FileTest {
    public static void listAllFiles(File dir) {//循环打出目录下所有的文件名
        if (dir == null || dir.exists()) {
            return;
        }
        if (dir.isFile()) {//如果是文件，那么打出文件名字
            System.out.println(dir.getName());
            return;
        }
        for (File file : dir.listFiles()) {//如果是文件夹，递归打出
            listAllFiles(file);
        }
    }
}
