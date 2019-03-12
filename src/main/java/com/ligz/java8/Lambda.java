package com.ligz.java8;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Lambda表达式
 * author:ligz
 */
public class Lambda {

    public static void main(String[] args) {
        List<String> names = Arrays.asList("peter", "anna", "mike", "xenia");

        Collections.sort(names, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return b.compareTo(a);
            }
        });

        Collections.sort(names, (a, b) -> b.compareTo(a));//一行方法的方法体可以跳过{}和参数类型，使它变得更短：
    }
}
