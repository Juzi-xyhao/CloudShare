package com.CloudShare.annotation;


import com.CloudShare.entity.enums.VerifyRegexEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})//作用于参数和属性
public @interface VerifyParam {  //以下都是自定义注解的属性
    /**
     * 校验正则
     *
     * @return
     */
    VerifyRegexEnum regex() default VerifyRegexEnum.NO;

    /**
     * 最小长度
     *
     * @return
     */
    int min() default -1;

    /**
     * 最大长度
     *
     * @return
     */
    int max() default -1;

    boolean required() default false;
}
