package com.ligz.spring.aop.proxy;

import com.ligz.spring.aop.proxy.*;
import org.junit.Test;

/**
 * author:ligz
 */
public class SimpleAOPTest {
    @Test
    public void getProxy() throws Exception{
        MethodInvocation logTask = () -> System.out.println("log task start");
        HelloServiceImpl helloServiceImpl = new HelloServiceImpl();

        //创建一个advice
        Advice beforAdvice = new BeforeAdvice(helloServiceImpl, logTask);

        HelloService helloServiceImplProxy = (HelloService) SimpleAOP.getProxy(helloServiceImpl, beforAdvice);

        helloServiceImplProxy.sayHelloWorld();
    }
}
