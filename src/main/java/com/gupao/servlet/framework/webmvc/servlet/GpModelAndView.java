package com.gupao.servlet.framework.webmvc.servlet;

import java.util.Map;

public class GpModelAndView {

    private String viewName;

    private Map<String, ?> model;

    public GpModelAndView(String viewName) {
        this.viewName = viewName;
    }

    public GpModelAndView(String viewName, Map<String, ?> model) {
        this.viewName = viewName;
        this.model = model;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Map<String, ?> getModel() {
        return model;
    }

    public void setModel(Map<String, ?> model) {
        this.model = model;
    }
}
