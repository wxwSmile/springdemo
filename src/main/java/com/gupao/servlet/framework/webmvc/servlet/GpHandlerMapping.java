package com.gupao.servlet.framework.webmvc.servlet;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class GpHandlerMapping {

    private Pattern pattern;
    private Method method;
    private Object controller;

    public GpHandlerMapping( Pattern pattern, Object controller, Method method) {
        this.method = method;
        this.controller = controller;
        this.pattern = pattern;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }
}
