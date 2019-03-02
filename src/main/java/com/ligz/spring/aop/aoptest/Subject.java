package com.ligz.spring.aop.aoptest;

/**
 * 定义主题接口，这些接口的方法可以成为我们的连接点
 * author:ligz
 */
public interface Subject {
    //登陆
    public void login();

    //下载
    public void download();
}
