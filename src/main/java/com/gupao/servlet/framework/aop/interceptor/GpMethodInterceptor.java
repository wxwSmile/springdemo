package com.gupao.servlet.framework.aop.interceptor;

public interface GpMethodInterceptor {

    Object invoke(GpMethodInvocation invocation) throws Throwable;
}
