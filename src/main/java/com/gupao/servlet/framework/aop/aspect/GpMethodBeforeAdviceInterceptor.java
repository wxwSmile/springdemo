package com.gupao.servlet.framework.aop.aspect;

import com.gupao.servlet.framework.aop.interceptor.GpMethodInterceptor;
import com.gupao.servlet.framework.aop.interceptor.GpMethodInvocation;

import java.lang.reflect.Method;

public class GpMethodBeforeAdviceInterceptor extends GpAbstractAspectAdvice implements GpMethodInterceptor, GpAdvice {

    private GpJoinPoint joinPoint;

    public GpMethodBeforeAdviceInterceptor(Method aspectMethod, Object aspectTarget) {
        super(aspectMethod, aspectTarget);
    }

    private void before(Method method, Object[] args, Object target) throws Throwable {
        //传给织入的参数
//        method.invoke(target);
        super.invokeAdviceMethod(this.joinPoint, null, null);
    }

    public Object invoke(GpMethodInvocation mi) throws Throwable {
        this.joinPoint = mi;
        before(mi.getMethod(), mi.getArguments(), mi.getThis());
        return null;
    }
}
