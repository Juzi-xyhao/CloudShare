package com.CloudShare.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})  //作用于方法上
@Retention(RetentionPolicy.RUNTIME)  //程序执行时生效
@Documented
@Mapping
public @interface GlobalInterceptor {  //以下都是自定义注解的属性

    /**
     * 校验登录
     *
     * @return
     */
    boolean checkLogin() default true;

    /**
     * 校验参数
     *
     * @return
     */
    boolean checkParams() default false;

    /**
     * 校验管理员
     *
     * @return
     */
    boolean checkAdmin() default false;
}
