package com.gupao.servlet.framework.beans.support;

import com.gupao.servlet.framework.beans.config.GpBeanDefinition;
import com.gupao.servlet.framework.context.GpAbstractApplicationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GpDefaulListableBeanFactory extends GpAbstractApplicationContext {

    //伪ioc容器
    protected final Map<String, GpBeanDefinition> beanDefinitionMap = new ConcurrentHashMap(256);
}
