package com.gupao.servlet.framework.aop.aspect;

import java.lang.reflect.Method;

public interface GpJoinPoint {

    Object getThis();

    Object[] getArguments();

    Method getMethod();

}
