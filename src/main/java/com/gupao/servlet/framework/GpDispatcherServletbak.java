package com.gupao.servlet.framework;

import com.gupao.servlet.framework.annotation.*;

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

public class GpDispatcherServletbak extends HttpServlet {

    //保存配置文件扫描到的内容
    private Properties contextConfig = new Properties();

    //保存扫描到的类
    private List<String> classNameList = new ArrayList<String>();

    //ioc容器
    private Map<String, Object> ioc = new HashMap<String, Object>();

    //保存url对应的mapping
    private Map<String, Method> handMapping = new HashMap<String, Method>();

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
        String url = req.getRequestURI().replaceAll("/gupaosrping", "");//绝对路径
        //处理成相对路径
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");

        if (!this.handMapping.containsKey(url)) {
            response.getWriter().write("    404 Not Found");
            return;
        }

        Method method = this.handMapping.get(url);
        //获取参数类型列表
        Class<?>[] paramsType = method.getParameterTypes();

        //获取请求的参数
        Map<String, String[]> map = req.getParameterMap();

        //保存参数值
        Object [] paramValues = new Object[paramsType.length];

        for (int i = 0; i < paramsType.length; i++) {
            Class paramType = paramsType[i];
            if (paramType.equals(HttpServletRequest.class)) {
                paramValues[i] = req;
                continue;
            } else if (paramType.equals(HttpServletResponse.class)) {
                paramValues[i] = response;
                continue;
            } else  {
                Annotation[][] pa = method.getParameterAnnotations();
                for (int j = 0; j < pa.length; j++) {
                    for (Annotation a : pa[j]) {
                        if (a instanceof GpRequestParam) {//拿到参数注解的值
                            String paramName = ((GpRequestParam) a).value();
                            if (map.containsKey(paramName.trim())) {//一个key对应一个数组 二维数组
//                                for (Map.Entry<String, String[]> param : map.entrySet()) {
                                    String value = Arrays.toString(map.get(paramName)).replaceAll("\\[|\\]", "").replaceAll("\\s", ",");
                                    paramValues[i] = convert(paramType, value);
//                                }
                            }
                        }
                    }
                }
//                for (Map.Entry<String, String[]> param : map.entrySet()) {
//                    String value = Arrays.toString((param.getValue())).replaceAll("\\[|\\]", "")
//                            .replaceAll("\\s", ",");
//                    paramValues[i] = value;
//                }
            }
        }
        //通过反射调用
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName), paramValues);
    }

    //http基于string传输
    private Object convert(Class<?> type, String value) {
        if (Integer.class == type) {
            return Integer.valueOf(value);
        }
        return value;
    }

    //配置阶段 初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //扫描相关类
        doScanner(contextConfig.getProperty("scanPackage"));

        //初始化相关类放到Ioc容器中
        doInstance();

        //完成依赖注入
        doAutowired();

        //初始化handmapping
        initHandMapping();

        System.out.println("gp srpingframework complete");

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
                handMapping.put(url, method);
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
}
