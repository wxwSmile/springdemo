package com.gupao.servlet.framework.context;

/**
 * 通过解耦的方式获得ioc容器的顶层设计
 * 通过监听器去扫描 只要实现了此接口 就调用 setApplicationContext
 * 方法从ioc容器注入到目标类中
 */
public interface GpApplicationContextAware {

    void setApplicationContext(GpApplicationContext var1);
}
