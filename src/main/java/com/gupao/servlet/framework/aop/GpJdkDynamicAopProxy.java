package com.gupao.servlet.framework.aop;

import com.gupao.servlet.framework.aop.interceptor.GpMethodInvocation;
import com.gupao.servlet.framework.aop.support.GpAdvisedSupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class GpJdkDynamicAopProxy implements GpAopProxy, InvocationHandler {

    private GpAdvisedSupport support;

    public GpJdkDynamicAopProxy(GpAdvisedSupport support) {
        this.support = support;
    }

    public Object getProxy() {
        return getProxy(this.support.getTargetClass().getClassLoader());
    }

    public Object getProxy(ClassLoader classLoader) {
        return  Proxy.newProxyInstance(classLoader, this.support.getTargetClass().getInterfaces(), this);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        List<Object> interceptorsAndDynamicMethodMatchers = this.support.getInterceptorsAndDynamicInterceptionAdvice(method, this.support.getTargetClass());
        GpMethodInvocation invocation = new GpMethodInvocation(proxy, this.support.getTargetClass(), method, args, this.support.getTargetClass(), interceptorsAndDynamicMethodMatchers);
        return invocation.proceed();
    }
}
