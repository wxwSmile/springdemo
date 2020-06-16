package com.gupao.servlet.framework.aop.aspect;

import com.gupao.servlet.framework.aop.interceptor.GpMethodInterceptor;
import com.gupao.servlet.framework.aop.interceptor.GpMethodInvocation;

import java.lang.reflect.Method;

public class GpAfterReturningAdviceInterceptor extends GpAbstractAspectAdvice implements GpMethodInterceptor, GpAdvice {

    private GpJoinPoint joinPoint;

    public GpAfterReturningAdviceInterceptor(Method aspectMethod, Object aspectTarget) {
        super(aspectMethod, aspectTarget);
    }

    public Object invoke(GpMethodInvocation mi) throws Throwable {
        Object retVal = mi.proceed();
        this.joinPoint = mi;
        this.afterReturning(retVal, mi.getMethod(), mi.getArguments(),mi.getThis());
        return null;
    }

    private void  afterReturning(Object retVal, Method method, Object[] arguments,Object object) throws Throwable {
        super.invokeAdviceMethod(this.joinPoint, retVal, null);
    }
}
