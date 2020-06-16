package com.gupao.servlet.framework.beans.config;

public class GpBeanDefinition {

    private String beanClassName;

    private boolean isLazyInit = false;

    private String factoryBeanName;

    private boolean isSingleton = true;

    public boolean isSingleton() {
        return isSingleton;
    }

    public void setSingleton(boolean singleton) {
        isSingleton = singleton;
    }

    public String getBeanClassName() {
        return beanClassName;
    }

    public void setBeanClassName(String beanClassName) {
        this.beanClassName = beanClassName;
    }

    public boolean isLazyInit() {
        return isLazyInit;
    }

    public void setLazyInit(boolean lazyInit) {
        isLazyInit = lazyInit;
    }

    public String getFactoryBeanName() {
        return factoryBeanName;
    }

    public void setFactoryBeanName(String factoryBeanName) {
        this.factoryBeanName = factoryBeanName;
    }
}
