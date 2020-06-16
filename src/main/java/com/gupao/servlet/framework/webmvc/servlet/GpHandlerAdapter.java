package com.gupao.servlet.framework.webmvc.servlet;

import com.gupao.servlet.framework.GpDispatcherServlet;
import com.gupao.servlet.framework.annotation.GpRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GpHandlerAdapter {

    public boolean support(Object handler) {return (handler instanceof GpHandlerMapping);}

    public GpModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws InvocationTargetException, IllegalAccessException {
        GpHandlerMapping handlerMapping = (GpHandlerMapping)handler;

        //形参列表 把方法的参数形参列表和request 所在的顺序 一一对应
         Map<String, Integer> paramIndexMapping = new HashMap<String, Integer>();

            Annotation[][] pa = handlerMapping.getMethod().getParameterAnnotations();
            for (int i = 0; i < pa.length; i++) {
                for (Annotation a : pa[i]) {
                    if (a instanceof GpRequestParam) {//拿到参数注解的值
                        String paramName = ((GpRequestParam) a).value();
                        if (!"".equals(paramName.trim())) {//一个key对应一个数组 二维数组
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }

            Class<?>[] paramType =  handlerMapping.getMethod().getParameterTypes();
            for (int i = 0; i < paramType.length; i++) {
                Class<?> type = paramType[i];
                if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                    paramIndexMapping.put(type.getName(), i);
                }
            }

        //获得形参列表
        Map<String, String[]> stringMap = request.getParameterMap();
        Object [] paramValues = new Object[pa.length];

        for (Map.Entry<String, String[]> param : stringMap.entrySet()) {
            String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "")
                    .replaceAll("\\s", ",");

            if (!paramIndexMapping.containsKey(param.getKey())) { continue;}

            int index = paramIndexMapping.get(param.getKey());
            paramValues[index] = convert(paramType[index], value);
        }

        //请求参数是否有httpservletRequest httpservletResponse
        if (paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
            int reqIndex = paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = request;
        }

        if (paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
            int respIndex = paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = response;
        }

        Object result = handlerMapping.getMethod().invoke(handlerMapping.getController(), paramValues);
        if (result == null || result instanceof Void) {return null;}

       boolean isModelAndView = handlerMapping.getMethod().getReturnType() == GpModelAndView.class;
        if (isModelAndView) {
            return (GpModelAndView) result;
        }
        return null;
    }

    //http基于string传输
    private Object convert(Class<?> type, String value) {
        if (Integer.class == type) {
            return Integer.valueOf(value);
        } else if (Double.class  == type) {
            return Double.valueOf(value);
        }
        return value;
    }

}
