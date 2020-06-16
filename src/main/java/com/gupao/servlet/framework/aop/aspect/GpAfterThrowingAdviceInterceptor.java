package com.gupao.servlet.framework.aop.aspect;

import com.gupao.servlet.framework.aop.interceptor.GpMethodInterceptor;
import com.gupao.servlet.framework.aop.interceptor.GpMethodInvocation;

import java.lang.reflect.Method;

public class GpAfterThrowingAdviceInterceptor extends GpAbstractAspectAdvice implements GpMethodInterceptor {

    private GpJoinPoint joinPoint;

    private String throwName;

    public GpAfterThrowingAdviceInterceptor(Method aspectMethod, Object aspectTarget) {
        super(aspectMethod, aspectTarget);
    }

    public Object invoke(GpMethodInvocation mi) throws Throwable {
        try {
            return mi.proceed();
        } catch (Exception e) {
            invokeAdviceMethod(mi, null, e.getCause());
            throw e;
        }
    }

    public void setThrowName(String throwName) {
        this.throwName = throwName;
    }
}
