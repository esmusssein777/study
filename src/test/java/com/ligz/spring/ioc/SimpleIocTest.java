package com.ligz.spring.ioc;

import org.junit.Test;

/**
 * author:ligz
 */
public class SimpleIocTest {
    @Test
    public void getBean() throws Exception {
        String location = SimpleIOC.class.getClassLoader().getResource("ioc.xml").getFile();
        SimpleIOC bf = new SimpleIOC(location);
        Wheel wheel = (Wheel) bf.getBean("wheel");
        Car car = (Car) bf.getBean("car");
        System.out.println(wheel);
        System.out.println(car);
    }
}
