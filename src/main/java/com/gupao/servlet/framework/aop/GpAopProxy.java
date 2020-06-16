package com.gupao.servlet.framework.aop;

public interface GpAopProxy {

    public Object getProxy();

    Object getProxy(ClassLoader classLoader);
}
