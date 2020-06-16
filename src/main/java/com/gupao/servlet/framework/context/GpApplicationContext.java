package com.gupao.servlet.framework.context;


import com.gupao.servlet.framework.annotation.GpAutowried;
import com.gupao.servlet.framework.annotation.GpController;
import com.gupao.servlet.framework.annotation.GpService;
import com.gupao.servlet.framework.aop.GpAopProxy;
import com.gupao.servlet.framework.aop.GpCglibAopProxy;
import com.gupao.servlet.framework.aop.GpJdkDynamicAopProxy;
import com.gupao.servlet.framework.aop.config.GpAopConfig;
import com.gupao.servlet.framework.aop.support.GpAdvisedSupport;
import com.gupao.servlet.framework.beans.GpBeanFactory;
import com.gupao.servlet.framework.beans.GpBeanWrapper;
import com.gupao.servlet.framework.beans.config.GpBeanDefinition;
import com.gupao.servlet.framework.beans.config.GpBeanPostProcessor;
import com.gupao.servlet.framework.beans.support.GpBeanDefinitonReader;
import com.gupao.servlet.framework.beans.support.GpDefaulListableBeanFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 从ioc di  aop  mvc
 */
public class GpApplicationContext extends GpDefaulListableBeanFactory implements GpBeanFactory {

    private String[] configLocation;
    private GpBeanDefinitonReader reader;

    //单例ioc容器
    private Map<String, Object> singLetonObjects = new HashMap<String, Object>();

    //普通的ioc 容器
    private Map<String, GpBeanWrapper> factoryBeanInstanceCache = new HashMap<String, GpBeanWrapper>();

    public GpApplicationContext(String  ... configLocation) throws Exception {
        this.configLocation = configLocation;
        refresh();
    }


    @Override
    protected void refresh() throws Exception {
        //1 定位 配置文件
        reader = new GpBeanDefinitonReader(this.configLocation);

        //2 加载配置文件 扫描成相关的类 封装成beandefiniton
        List<GpBeanDefinition> beanDefinitions = reader.loadBeanDefinitions(); 

        //3 注册配置信息放到容器里面（伪ioc容器）
        doRegisterBeanDefinition(beanDefinitions);

        //4 把不是延时加载的类 初始化 注入
        doAutowried();
    }

    //只处理非延时加载
    private void  doAutowried() throws Exception {
        for (Map.Entry<String, GpBeanDefinition> beanDefinitionEntry : super.beanDefinitionMap.entrySet()) {
            String beanName = beanDefinitionEntry.getKey();
            if (!beanDefinitionEntry.getValue().isLazyInit()) {
                getBean(beanName);
            }
        }
    }

    private void doRegisterBeanDefinition(List<GpBeanDefinition> beanDefinitions) {
        for (GpBeanDefinition beanDefinition : beanDefinitions) {
            super.beanDefinitionMap.put(beanDefinition.getFactoryBeanName(), beanDefinition);
        }
    }

    public Object getBean(Class<?> clazz) throws Exception {
      return getBean(clazz.getName());
    }

    public Object getBean(String beanName) throws Exception {
        GpBeanDefinition gpBeanDefinition = this.beanDefinitionMap.get(beanName);

        //1 初始化
        Object instance = null;

        //bean前置处理器
        GpBeanPostProcessor gpBeanPostProcessor = new GpBeanPostProcessor();
        gpBeanPostProcessor.postProcessBeforeInitialization(instance, beanName);

        instance = instantiateBean(beanName,gpBeanDefinition);
        //2 把对象封装到beanwrapper中 singloteobjects
        GpBeanWrapper wrapper = new GpBeanWrapper(instance);

        //创建一个代理的策略

        this.factoryBeanInstanceCache.put(beanName, wrapper);

        //bean后置处理器
        gpBeanPostProcessor.postProcessAfterInitialization(instance, beanName);

        //3 注入
        populateBean(beanName, new GpBeanDefinition(), wrapper);

        return this.factoryBeanInstanceCache.get(beanName).getWrappedInstance();
    }

    private void populateBean(String beanName, GpBeanDefinition gpBeanDefinition, GpBeanWrapper gpBeanWrapper) throws IllegalAccessException {
        Object instance = gpBeanWrapper.getWrappedInstance();

        //只有加了注解的类 才执行依赖注入
        Class<?> clazz = gpBeanWrapper.getWrappedClass();
        if (!(clazz.isAnnotationPresent(GpController.class ) || clazz.isAnnotationPresent(GpService.class))){return;}

        //拿到所有属性
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(GpAutowried.class)) {continue;}

            GpAutowried autowried = field.getAnnotation(GpAutowried.class);
            String value = autowried.value();
            if ("".equals(value)) {
                value = field.getType().getName();
            }
            field.setAccessible(true);
            if (this.factoryBeanInstanceCache.get(value) == null) {continue;}
            field.set(instance, this.factoryBeanInstanceCache.get(value).getWrappedInstance());
        }
    }

    private Object instantiateBean(String beanName, GpBeanDefinition gpBeanDefinition) {
        //1 拿到要实例化对象的类名
       String className = gpBeanDefinition.getBeanClassName();

        //2 反射实例化
        Object instance = null ;
       try {

          if (this.singLetonObjects.containsKey(className)) {
              instance = this.singLetonObjects.get(className);
          } else {
              Class<?> clazz = Class.forName(className);
              instance = clazz.newInstance();
              GpAdvisedSupport config = instantionAopConfig(gpBeanDefinition);
              config.setTargetClass(clazz);
              config.setTarget(instance);

              //符合pointCut原则的话 将被代理
              if (config.pointCutMatch()) {
                  instance = createProxy(config).getProxy();
              }

              this.singLetonObjects.put(className, instance);
              this.singLetonObjects.put(gpBeanDefinition.getFactoryBeanName(), instance);
          }

       } catch (Exception e) {
          e.printStackTrace();
       }

        //3 把 beanwrapper封装到ioc容器中
        return instance;
    }

    private GpAopProxy createProxy(GpAdvisedSupport config) {
        Class targetClass = config.getTargetClass();
        if (targetClass.getInterfaces().length > 0) {
            return  new GpJdkDynamicAopProxy(config);
        }
        return new GpCglibAopProxy(config);
    }

    private GpAdvisedSupport instantionAopConfig(GpBeanDefinition gpBeanDefinition) {
        GpAopConfig config = new GpAopConfig();
        config.setPointCut(this.reader.getConfig().getProperty("pointCut"));
        config.setAspectBefore(this.reader.getConfig().getProperty("aspectBefore"));
        config.setAspectAfter(this.reader.getConfig().getProperty("aspectAfter"));
        config.setAspectClass(this.reader.getConfig().getProperty("aspectClass"));
        config.setAspectAfterThrow(this.reader.getConfig().getProperty("aspectAfterThrow"));
        config.setAspectAfterThrowingName(this.reader.getConfig().getProperty("aspectAfterThrowingName"));
        return new GpAdvisedSupport(config);
    }

    public String[] getBeanDefinitonNames() {
        return this.beanDefinitionMap.keySet().toArray(new String[this.beanDefinitionMap.size()]);
    }

    public int getBeanDefinitonCount() {
        return this.beanDefinitionMap.size();
    }

    public Properties getConfig() {
        return this.reader.getConfig();
    }
}
