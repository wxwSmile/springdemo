package com.gupao.servlet.framework.annotation;


import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GpAutowried {
    String value() default "";
}
