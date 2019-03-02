package com.ligz.spring.aop.proxy;

import java.lang.reflect.Proxy;

/**
 * author:ligz
 */
public class SimpleAOP {
    public static Object getProxy(Object bean, Advice advice) {
        return Proxy.newProxyInstance(SimpleAOP.class.getClassLoader(),bean.getClass().getInterfaces(), advice);
    }
}
