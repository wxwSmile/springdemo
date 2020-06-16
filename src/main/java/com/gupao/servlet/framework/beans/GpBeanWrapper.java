package com.gupao.servlet.framework.beans;

public class GpBeanWrapper {

    private Object wrappedInstance;

    private  Class<?> wrappedClass;

    public GpBeanWrapper(Object wrappedInstance) {
        this.wrappedInstance = wrappedInstance;
        this.wrappedClass = wrappedInstance.getClass();
    }

    public Object getWrappedInstance() {
        return this.wrappedInstance;
    }

    public Class<?> getWrappedClass() {
        return this.wrappedClass.getClass();
    }
}
