package com.ligz.spring.aop.proxy;

/**
 * author:ligz
 */
public class HelloServiceImpl implements HelloService{
    @Override
    public void sayHelloWorld() {
        System.out.println("hello world!");
    }
}
