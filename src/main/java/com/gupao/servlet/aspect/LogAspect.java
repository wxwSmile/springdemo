package com.gupao.servlet.aspect;

import com.gupao.servlet.framework.aop.aspect.GpJoinPoint;

public class LogAspect {

    public void before(GpJoinPoint joinPoint) {
        System.out.println("before advice: " + joinPoint.getThis() + "method: " + joinPoint.getMethod().getName());
    }

    public void after(GpJoinPoint joinPoint) {
        System.out.println("after advice: " + joinPoint.getThis() + "method: " + joinPoint.getMethod().getName());
    }

    public void afterThrowing(GpJoinPoint joinPoint) {
        System.out.println("afterThrowing advice: " + joinPoint.getThis() + "method: " + joinPoint.getMethod().getName());
    }
}
