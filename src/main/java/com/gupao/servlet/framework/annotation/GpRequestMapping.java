package com.gupao.servlet.framework.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GpRequestMapping {
    String value() default "";
    GpRequestMethod[] method() default {};
}
