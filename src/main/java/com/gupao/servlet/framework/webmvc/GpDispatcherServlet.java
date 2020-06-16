package com.gupao.servlet.framework.webmvc;

import com.gupao.servlet.framework.annotation.GpController;
import com.gupao.servlet.framework.annotation.GpRequestMapping;
import com.gupao.servlet.framework.beans.support.GpBeanDefinitonReader;
import com.gupao.servlet.framework.context.GpApplicationContext;
import com.gupao.servlet.framework.webmvc.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GpDispatcherServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(GpDispatcherServlet.class);

    private final String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";

    private GpApplicationContext context;

    private List<GpHandlerMapping> handlerMappings = new ArrayList<GpHandlerMapping>();

    private Map<GpHandlerMapping, GpHandlerAdapter> handlerAdapter = new HashMap<GpHandlerMapping, GpHandlerAdapter>();

    private GpBeanDefinitonReader reader = new GpBeanDefinitonReader();

    private List<GpViewResover> viewResovers = new ArrayList<GpViewResover>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            this.doDispatcher(req, resp);
    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) {
        // 1通过 request 拿到 url 匹配handlermapping
        GpHandlerMapping handler = getHandler(req);

        if (handler == null) {//404
            processDispatchResult(req,resp, new GpModelAndView("404"));
            return;
        }
        //准备调用参数
        GpHandlerAdapter handlerAdapter = getHandlerAdapter(handler);

        //真正调用 返回modelandview
        GpModelAndView modelAndView = null;
        try {
            modelAndView = handlerAdapter.handle(req, resp, handler);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        processDispatchResult(req, resp, modelAndView);

    }

    private void processDispatchResult(HttpServletRequest req, HttpServletResponse resp, GpModelAndView mv) {
        //modelandvew 变成一个html outpputstream json framwork 等等
        if (null == mv) {return;}

        if (this.viewResovers.isEmpty()){return;}

        for (GpViewResover viewResover : this.viewResovers) {
           GpView view = viewResover.resoverViewName(mv.getViewName(), null);
           view.render(mv.getModel(), req, resp);
           return;
        }
    }

    private GpHandlerAdapter getHandlerAdapter(GpHandlerMapping handler) {
        if (this.handlerAdapter.isEmpty()){ return null;}
        GpHandlerAdapter ha = this.handlerAdapter.get(handler);
        if (ha.support(handler)) {
            return ha;
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        logger.info("access init method");
        //初始化GpApplicationContext
        try {
            context = new GpApplicationContext(config.getInitParameter(CONTEXT_CONFIG_LOCATION));
            initStrategies(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void initStrategies(GpApplicationContext context) {
        this.initMultipartResolver(context);
        this.initLocaleResolver(context);
        this.initThemeResolver(context);
        this.initHandlerMappings(context);
        this.initHandlerAdapters(context);
        this.initHandlerExceptionResolvers(context);
        this.initRequestToViewNameTranslator(context);
        this.initViewResolvers(context);
        this.initFlashMapManager(context);
    }

    private void initFlashMapManager(GpApplicationContext context) {
    }

    private void  initViewResolvers(GpApplicationContext context) {
        //拿到模板存放目录
        String templateRoot = context.getConfig().getProperty("templateRoot");
        String templateRootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();
        File files = new File(templateRootPath);

        for (File file : files.listFiles()) {
            this.viewResovers.add(new GpViewResover(templateRoot));
        }
    }

    private void initRequestToViewNameTranslator(GpApplicationContext context) {
    }

    private void initHandlerExceptionResolvers(GpApplicationContext context) {
    }

    //把一个handler 请求的参数对应 拿到 handlermapping 才能一一对应
    private void initHandlerAdapters(GpApplicationContext context) {
        for (GpHandlerMapping handlerMapping : handlerMappings) {
            this.handlerAdapter.put(handlerMapping,  new GpHandlerAdapter() );
        }
    }

    //url method controller 对应
    private void initHandlerMappings(GpApplicationContext context) {
        logger.info("access initHandlerMappings method");
        //拿到所有的bean
        String[] beanNames = context.getBeanDefinitonNames();
        try {
            for (String name : beanNames) {
                Object controller = context.getBean(name);
                Class<?> clazz = controller.getClass();
                if (!clazz.isAnnotationPresent(GpController.class)) { continue;}

                String baseUrl = "";
                if (clazz.isAnnotationPresent(GpRequestMapping.class)) {
                    GpRequestMapping requestMapping = clazz.getAnnotation(GpRequestMapping.class);
                    baseUrl = requestMapping.value().trim();//类上的url;
                }

                //默认获取所有的public方法
                for (Method method : clazz.getMethods()) {
                    if (!method.isAnnotationPresent(GpRequestMapping.class)) {continue;}
                    GpRequestMapping requestMapping = method.getAnnotation(GpRequestMapping.class);
                    String url = (baseUrl + "/" + requestMapping.value().trim().replaceAll("\\*", ".*")).replaceAll("/+", "/");
                    Pattern pattern = Pattern.compile(url);
                    this.handlerMappings.add(new GpHandlerMapping(pattern, controller, method));
                   logger.info("url:" + url + "method:" + method + "controller:" + controller);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initThemeResolver(GpApplicationContext context) {
    }

    private void initLocaleResolver(GpApplicationContext context) {
    }

    private void initMultipartResolver(GpApplicationContext context) {
    }

    private GpHandlerMapping getHandler(HttpServletRequest req) {
        if (this.handlerMappings.isEmpty()) {return null;}
        String url = req.getRequestURI().replaceAll("/gupaospring", "");//绝对路径这里暂时写死
        //处理成相对路径
        String contextPath = req.getContextPath();
        logger.info("contextPath : [{}]", contextPath);

        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        for (GpHandlerMapping handlerMapping : this.handlerMappings) {
            Matcher matcher = handlerMapping.getPattern().matcher(url);
            if (!matcher.matches()) { continue;}
            return handlerMapping;
        }
        return null;
    }
}
