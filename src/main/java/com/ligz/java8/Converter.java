package com.ligz.java8;

/**
 * 函数式接口(Functional Interfaces)
 * author:ligz
 */
@FunctionalInterface
interface Converter<F, T> {
    T convert(F from);
}

class A{
    public static void main(String[] args) {
        Converter<String, Integer> converter = (from) -> Integer.valueOf(from);
        Integer converted = converter.convert("123");
        System.out.println(converted);    // 123

        //方法和构造函数引用。上面的例子代码可以进一步简化，利用静态方法引用：
        Converter<String, Integer> converter2 = Integer::valueOf;
        Integer converted2 = converter.convert("123");
        System.out.println(converted);   // 123
    }
}