package com.gupao.servlet.framework.beans;

public interface GpBeanFactory {

    /**
     * 从ioc容器获取bean
     * @param beanName
     * @return
     */
   public Object getBean(String beanName) throws Exception;

//   public Class<?> getBeanClass(Class<?> beanClass) throws Exception;

}
