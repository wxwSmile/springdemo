package com.gupao.servlet.framework.beans.support;

import com.gupao.servlet.framework.annotation.GpController;
import com.gupao.servlet.framework.annotation.GpService;
import com.gupao.servlet.framework.beans.config.GpBeanDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class GpBeanDefinitonReader {
    //加载properties文件
    private Properties config = new Properties();

    //包路径
    private final String SCAN_PACKAGE = "scanPackage";

    //存放实体类
    private List<String> registerClassNames = new ArrayList<String>();

    public Properties getConfig() {
        return this.config;
    }

    public GpBeanDefinitonReader() {
    }

    public GpBeanDefinitonReader(String ... configLocation) {
        //通过url指定文件转化成文件流
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configLocation[0].replace("classpath:", ""));
//        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configLocation.toString());
        try {
            config.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //通过配置文件扫描到实体类
        doScanner(config.getProperty(SCAN_PACKAGE));
    }

    private void doScanner(String scanPackage) {
        //scanPackage=com.gupao.servlet包路径 变成文件路径
        //classpath
        URL url = this.getClass().getResource("/" + scanPackage.replaceAll("\\.", "/"));
//        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classpath = new File(url.getFile());
        for (File file : classpath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")){continue;}
                String className = (scanPackage + "." + file.getName().replace(".class", ""));
                registerClassNames.add(className);
            }
        }
    }

    //把配置文件中的信息转换成GpBeanDefinition
    public List<GpBeanDefinition> loadBeanDefinitions(String... var1) {
        List<GpBeanDefinition> result = new ArrayList<GpBeanDefinition>();
        try {
            for (String className : registerClassNames) {
                Class<?> beanClass = Class.forName(className);
                if (beanClass.isInterface()){continue;}
                if (!(beanClass.isAnnotationPresent(GpController.class) || beanClass.isAnnotationPresent(GpService.class))){continue;}

                result.add(doCreateBeanDefinition(beanClass.getName(), toLowerFirstCase(beanClass.getSimpleName())));
                result.add(doCreateBeanDefinition(beanClass.getName(), beanClass.getName()));
//                result.add(doCreateBeanDefinition(beanClass.getSimpleName(), beanClass.getName()));
//                result.add(doCreateBeanDefinition(beanClass.getName(), beanClass.getName()));
                Class<?> [] interfaces = beanClass.getInterfaces();
                for (Class<?> i : interfaces) {
                    result.add(doCreateBeanDefinition(beanClass.getName(), i.getName()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

     return result;
   }

   private GpBeanDefinition doCreateBeanDefinition(String className, String beanName) {
        try {
            GpBeanDefinition beanDefinition = new GpBeanDefinition();
            beanDefinition.setBeanClassName(className);
            beanDefinition.setFactoryBeanName(beanName);

            return beanDefinition;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
   }

    private String toLowerFirstCase(String simpleName) {
        char [] chars = simpleName.toCharArray();
        //加32是因为大小写字母的ascii相差32 大写字母的asci小于小写字母的ascII
        chars[0] += 32;
        return String.valueOf(chars);
    }
}
