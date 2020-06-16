package com.gupao.servlet.framework.aop.support;

import com.gupao.servlet.framework.aop.aspect.GpAfterReturningAdviceInterceptor;
import com.gupao.servlet.framework.aop.aspect.GpAfterThrowingAdviceInterceptor;
import com.gupao.servlet.framework.aop.aspect.GpMethodBeforeAdviceInterceptor;
import com.gupao.servlet.framework.aop.config.GpAopConfig;
import javafx.scene.Parent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GpAdvisedSupport {

    private Class<?> targetClass;

    private Object target;

    private GpAopConfig config;

    private Pattern pattern;

    public Map<Method, List<Object>> methodCache;

    public GpAdvisedSupport(GpAopConfig config) {
        this.config = config;
    }

    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, Class<?> targetClass) {
        List<Object> cached = methodCache.get(method);
        if (cached == null) {
            try {
                Method m = targetClass.getMethod(method.getName(), method.getParameterTypes());
                cached = methodCache.get(m);
                this.methodCache.put(m, cached);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

        }
        return cached;
    }

    public Class<?> getTargetClass() {
        return this.targetClass;
    }

    public Object getTarget() {
        return this.target;
    }

    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
        parse();
    }

    private void parse() {
        String pointCut = config.getPointCut()
                .replaceAll("\\.", "\\\\.")
                .replaceAll("\\\\.\\*", ".*")
                .replaceAll("\\(", "\\\\(")
                .replaceAll("\\(", "\\\\)");
        //匹配规则
        String pointCutForClassReg = pointCut.substring(0, pointCut.lastIndexOf("\\(") - 4);
        pattern = Pattern.compile("class" + pointCutForClassReg.substring(pointCutForClassReg.lastIndexOf(" " + 1)));

        Pattern pattern = Pattern.compile(pointCut);
        methodCache = new HashMap<Method, List<Object>>();
        try {
            Map<String, Method> aspectMethods = new HashMap<String, Method>();
            Class aspectClass = Class.forName(this.config.getAspectClass());
            for (Method method : aspectClass.getMethods()) {
                aspectMethods.put(method.getName(), method);
            }
            for (Method method : this.getTargetClass().getMethods()) {
                String methodString = method.toString();
                if (methodString.contains("throws")) {
                    methodString = methodString.substring(0, methodString.lastIndexOf("throws")).trim();
                }

                Matcher macher = pattern.matcher(methodString);
                if (macher.matches()) {
                    List<Object> advices = new LinkedList<Object>();
                    //包装成methodinterceptor before after afterthrowing
                    if (!(null == config.getAspectBefore() || "".equals(config.getAspectBefore()))) {
                        //创建advice 对象
                        try {
                            advices.add(new GpMethodBeforeAdviceInterceptor(aspectMethods.get(config.getAspectBefore()), aspectClass.newInstance()));
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!(null == config.getAspectAfter() || "".equals(config.getAspectAfter()))) {
                        //创建advice 对象
                        try {
                            advices.add(new GpAfterReturningAdviceInterceptor(aspectMethods.get(config.getAspectAfter()), aspectClass.newInstance()));
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!(null == config.getAspectAfterThrow() || "".equals(config.getAspectAfterThrow()))) {
                        //创建advice 对象
//                    advices.add(new Gp)
                        GpAfterThrowingAdviceInterceptor throwingAdvice = null;
                        try {
                            throwingAdvice = new GpAfterThrowingAdviceInterceptor(aspectMethods.get(config.getAspectAfterThrow()), aspectClass.newInstance());
                            throwingAdvice.setThrowName(config.getAspectAfterThrowingName());
                            advices.add(new GpAfterThrowingAdviceInterceptor(aspectMethods.get(config.getAspectAfterThrow()), aspectClass.newInstance()));
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    methodCache.put(method, advices);
                }
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public boolean pointCutMatch() {
        return pattern.matcher(this.targetClass.toString()).matches();
    }
}
