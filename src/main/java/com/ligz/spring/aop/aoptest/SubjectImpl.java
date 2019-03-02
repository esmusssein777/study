package com.ligz.spring.aop.aoptest;

/**
 * 定义实现类,这是代理模式中真正的被代理人（如果你有参与代购，这个就像你在代购中的角色）
 * author:ligz
 */
public class SubjectImpl implements Subject{
    @Override
    public void login() {

        System.err.println("借书中...");

    }
    @Override
    public void download() {

        System.err.println("下载中...");

    }
}
