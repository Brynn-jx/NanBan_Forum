package com.NanBan.annotation;

import com.NanBan.entity.enums.UserOperFrequencyTypeEnum;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface GlobalInterceptor {
    /**
     * 是否需要登陆
     */
    boolean checkLogin() default false;

    /**
     * 是否需要参数校验
     */
    boolean checkParams() default false;

    /**
     * 校验频次
     */
    UserOperFrequencyTypeEnum frequencyType() default UserOperFrequencyTypeEnum.NO_CHECK;
}
