package com.gupao.test;

import com.gupao.servlet.action.DemoAction;
import com.gupao.servlet.framework.context.GpApplicationContext;
import com.gupao.servlet.service.impl.DemoService;

public class Test {
    public static void main(String[] args) throws Exception {
        GpApplicationContext applicationContext = new GpApplicationContext("classpath:application.properties");
        System.out.println(applicationContext.getBean("demoAction"));
        System.out.println(applicationContext.getBean(DemoService.class));
    }
}
