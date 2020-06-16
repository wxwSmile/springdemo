package com.gupao.servlet.framework;

import com.gupao.servlet.framework.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;


public class GpDispatcherServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(GpDispatcherServlet.class);

    //保存配置文件扫描到的内容
    private Properties contextConfig = new Properties();

    //保存扫描到的类
    private List<String> classNameList = new ArrayList<String>();

    //ioc容器
    private Map<String, Object> ioc = new HashMap<String, Object>();

    //保存url对应的mapping
//    private Map<String, Method> handMapping = new HashMap<String, Method>();

        // 为啥不用map handmapping 本身的功能url methond对应 单一职责原则
    private List<HandMapping> handMapping = new ArrayList<HandMapping>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        //调用 运行阶段
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 exception " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse response) throws Exception{
        HandMapping handMapping = getHandler(req);
        if (handMapping == null) {
            response.getWriter().write("404 Not Found");
            return;
        }

        //获得形参列表
        Class<?> [] paramTypes = handMapping.getParamTyps();
        Object [] paramValues = new Object[paramTypes.length];

        Map<String, String[]> stringMap = req.getParameterMap();
            for (Map.Entry<String, String[]> param : stringMap.entrySet()) {
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "")
                        .replaceAll("\\s", ",");

                if (!handMapping.paramIndexMapping.containsKey(param.getKey())) { continue;}

                int index = handMapping.paramIndexMapping.get(param.getKey());
                paramValues[index] = convert(paramTypes[index], value);
            }

        //请求参数是否有httpservletRequest httpservletResponse
        if (handMapping.paramIndexMapping.containsKey(HttpServletRequest.class.getName())) {
            int reqIndex = handMapping.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
        }

        if (handMapping.paramIndexMapping.containsKey(HttpServletResponse.class.getName())) {
            int respIndex = handMapping.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = response;
        }

        Object object = handMapping.method.invoke(handMapping.controller, paramValues);

        if (object == null){return;}
        response.getWriter().write(object.toString());
    }

    private HandMapping getHandler(HttpServletRequest req) {
        if (handMapping.isEmpty()) {return null;}
        String url = req.getRequestURI().replaceAll("/gupaospring", "");//绝对路径这里暂时写死
        //处理成相对路径
        String contextPath = req.getContextPath();
        logger.info("contextPath : [{}]", contextPath);

        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        for (HandMapping handMapping : this.handMapping) {
           if (handMapping.getUrl().equals(url)) {
               return handMapping;
           }
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

    //配置阶段 初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置文件 模板模式
        logger.info("access init method.......");
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //扫描相关类
        doScanner(contextConfig.getProperty("scanPackage"));

        //初始化相关类放到Ioc容器中
        doInstance();

        //完成依赖注入
        doAutowired();

        //初始化handmapping
        initHandMapping();

        logger.info("gp srpingframework complete");

    }

    private void initHandMapping() {
        if (ioc.isEmpty()) {return;}

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
          Class<?> clazz = entry.getValue().getClass();
          if (!clazz.isAnnotationPresent(GpController.class)) {continue;}

          String baseUrl = "";
          if (clazz.isAnnotationPresent(GpRequestMapping.class)) {
                GpRequestMapping requestMapping = clazz.getAnnotation(GpRequestMapping.class);
                baseUrl = requestMapping.value().trim();//类上的url;
          }

          //默认获取所有的public方法
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(GpRequestMapping.class)) {continue;}
                GpRequestMapping requestMapping = method.getAnnotation(GpRequestMapping.class);
                String url = (baseUrl + "/" + requestMapping.value().trim()).replaceAll("/+", "/");
//                handMapping.put(url, method);
                this.handMapping.add(new HandMapping(url, method, entry.getValue()));
                System.out.println("url:" + url + "method:" + method + "controller:" + entry.getValue());
            }
        }
    }

    //自动注入
    private void doAutowired() {
        if (ioc.isEmpty()) {return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //declared 所有的特定的 字段 包括private protected 等字段
            Field [] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(GpAutowried.class)) {continue;}
                GpAutowried autowried = field.getAnnotation(GpAutowried.class);
                String beanName = autowried.value().trim();
                if ("".equals(beanName)) {
                    //接口类型 作为key 到ioc容器中取值
                    beanName = field.getType().getName();
                }
                //如果是public 意外的修饰符 强制赋值
                field.setAccessible(true);
                //反射动态给字段赋值
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    //注入到ioc容器中去
    private void doInstance() {
        if (classNameList.isEmpty()) {return;}

        try {
            for (String className : classNameList) {
                Class<?> clazz =  Class.forName(className);
                if (clazz.isAnnotationPresent(GpController.class)) {
                    Object instance = clazz.newInstance();
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(GpService.class)) {
                    GpService service = clazz.getAnnotation(GpService.class);
                    //beanName 为空
                    String beanName = service.value();
                    Object instance = clazz.newInstance();
                    if ("".equals(service.value())) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    ioc.put(beanName, instance);
                    //接口名称
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("the" + i.getName() + "is exits");
                        }
                        ioc.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String toLowerFirstCase(String simpleName) {
        char [] chars = simpleName.toCharArray();
        //加32是因为大小写字母的ascii相差32 大写字母的asci小于小写字母的ascII
        chars[0] += 32;
        return String.valueOf(chars);
    }


    private void doScanner(String scanPackage) {
        //scanPackage=com.gupao.servlet包路径 变成文件路径
        //classpath
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classpath = new File(url.getFile());
        for (File file : classpath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")){continue;}
                String className = (scanPackage + "." + file.getName().replace(".class", ""));
                classNameList.add(className);
            }
        }
    }

    private void doLoadConfig(String contextConfigLocation) {
        //直接从类路径下 spring主配置文件的路径 读取放到properties中
        //保存到内存中scanPackage=com.gupao.servlet
        logger.info("acccess doLoadConfig method .......");
        InputStream inputStream = null;
        inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //保存一个url和method对应关系handMapping策略模式
    //解决多个参数问题不然参数容易错位
    public class HandMapping {
        private String url;
        private Method method;
        private Object controller;
        private Class<?> [] paramTyps;

        //形参列表
        private Map<String, Integer> paramIndexMapping;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
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

        public Class<?>[] getParamTyps() {
            return paramTyps;
        }

        public void setParamTyps(Class<?>[] paramTyps) {
            this.paramTyps = paramTyps;
        }

        public HandMapping(String url, Method method, Object controller) {
            this.url = url;
            this.method = method;
            this.controller = controller;
            paramTyps = method.getParameterTypes();

            paramIndexMapping = new HashMap<String, Integer>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method) {
            Annotation[][] pa = method.getParameterAnnotations();
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

          Class<?>[] paramType =  method.getParameterTypes();
            for (int i = 0; i < paramType.length; i++) {
                Class<?> type = paramType[i];
                if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                    paramIndexMapping.put(type.getName(), i);
                }
            }
        }
    }
}
