package com.gupao.servlet.action;


import com.gupao.servlet.framework.annotation.GpAutowried;
import com.gupao.servlet.framework.annotation.GpController;
import com.gupao.servlet.framework.annotation.GpRequestMapping;
import com.gupao.servlet.framework.annotation.GpRequestParam;
import com.gupao.servlet.framework.webmvc.servlet.GpModelAndView;
import com.gupao.servlet.service.IDemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@GpController
@GpRequestMapping(value = "/demo")
public class DemoAction {
    private static final Logger logger = LoggerFactory.getLogger(DemoAction.class);

    @GpAutowried
    private IDemoService demoService;

    @GpRequestMapping(value = "/query")
    public GpModelAndView query(HttpServletRequest request, HttpServletResponse response,
                                @GpRequestParam("name") String name) {
        logger.info("access query query");
        try {
            response.getWriter().write(name);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @GpRequestMapping(value = "/add")
    public GpModelAndView add(HttpServletRequest request, HttpServletResponse response,
                        @GpRequestParam("a") String a,
                        @GpRequestParam("b") String b) {
        logger.info("access add add");
        String result = null;
        try {
            result = demoService.add(a, b);
            return new GpModelAndView(result);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> model = new HashMap<String, Object>();
            model.put("detail","java.lang.NullPointerException");
            model.put("stackTrace", e.getStackTrace());
            return new GpModelAndView("500.html", model);
        }
    }

    @GpRequestMapping(value = "/sub")
    public GpModelAndView sub(HttpServletRequest request, HttpServletResponse response,
                    @GpRequestParam("a") Double a,
                    @GpRequestParam("b")  Double b) {
        logger.info("access add sub");
        try {
            response.getWriter().write("a - b = " +  (a - b));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @GpRequestMapping(value = "/remove")
    public String remove(@GpRequestParam("id") Integer id) {
       logger.info("access add remove");
        return "id = " + id;
    }
}
