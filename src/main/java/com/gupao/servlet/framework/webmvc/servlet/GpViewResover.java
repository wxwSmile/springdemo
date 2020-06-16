package com.gupao.servlet.framework.webmvc.servlet;

import java.io.File;
import java.util.Locale;

public class GpViewResover {
    private final String DEFAULT_TEMPLATE_SUFFX = ".html";

    private File templateRootFile;

    private String viewName;

    public GpViewResover(String templateRoot) {
        String templateRootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();
        templateRootFile = new File(templateRootPath);
    }

    public GpView resoverViewName(String viewName, Locale locale) {
        if (null == viewName || "".equals(viewName.trim())) {return null;}
        viewName = viewName.endsWith(DEFAULT_TEMPLATE_SUFFX) ? viewName : (viewName + DEFAULT_TEMPLATE_SUFFX);
        File file = new File((templateRootFile + "/" + viewName).replaceAll("/+", "/"));
        return new GpView(file);
    }

    public File getTemplateRootFile() {
        return templateRootFile;
    }

    public void setTemplateRootFile(File templateRootFile) {
        this.templateRootFile = templateRootFile;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
}
