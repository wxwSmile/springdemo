package com.gupao.servlet.framework.aop.interceptor;

import com.gupao.servlet.framework.aop.aspect.GpJoinPoint;

import java.lang.reflect.Method;
import java.util.List;

public class GpMethodInvocation implements GpJoinPoint {

    private Object proxy;
    private Object target;
    private Method method;
    private Object[] arguments;
    private  Class<?> targetClass;
//    private Map<String, Object> userAttributes;
    private  List<?> interceptorsAndDynamicMethodMatchers;
    private int currentInterceptorIndex = -1;

    //定义就个索引 从-1 开始记录拦截器的位置
    public GpMethodInvocation(Object proxy, Object target, Method method, Object[] arguments, Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers) {
        this.proxy = proxy;
        this.target = target;
        this.targetClass = targetClass;
        this.method = method;
        this.arguments = arguments;
        this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
    }

    //真正执行拦截器中的方法
    public Object proceed() throws Throwable {
        if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
            return this.method.invoke(this.target, this.arguments);
        } else {
            Object interceptorOrInterceptionAdvice = this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
            if (interceptorOrInterceptionAdvice instanceof GpMethodInterceptor) {
                GpMethodInterceptor mi = (GpMethodInterceptor)interceptorOrInterceptionAdvice;
                return mi.invoke(this);
            } else {
                return proceed();
            }
        }
    }

    public Object getThis() {
        return this.target;
    }

    public Object[] getArguments() {
        return this.arguments;
    }

    public Method getMethod() {
        return this.method;
    }
}
