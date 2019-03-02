package com.ligz.spring.aop.aoptest;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * https://juejin.im/post/5aa7818af265da23844040c6
 * author:ligz
 */
public class Test {
    public static void main(String[] args) {
        ApplicationContext ctx =
                new ClassPathXmlApplicationContext("SpringAOP.xml");

        Subject subject1 = (Subject)ctx.getBean("SubjectImpl1");
        Subject subject2 = (Subject)ctx.getBean("SubjectImpl2");

        subject1.login();
        subject1.download();


        System.err.println("==================");




        subject1.login();
        subject1.download();




    }
}
