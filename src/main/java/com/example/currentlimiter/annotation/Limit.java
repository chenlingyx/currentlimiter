package com.example.currentlimiter.annotation;

import com.example.currentlimiter.enums.LimitTypeEnum;

import java.lang.annotation.*;

/**
 * @author : chenling
 * @ClassName : Limit
 * @Description : //TODO
 * @Date : 2019/6/12 18:44
 * @since : v1.0.0
 **/


@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Limit {

    /**
     * 资源名称
     */
    String name() default "";

    /**
     * 资源key
     * @return
     */
    String key() default "";


    /**
     * key的前缀
     * @return
     */
    String prefix() default "";

    /**
     * 给定的时间段
     * @return
     */
    int period();


    /**
     * 最多访问次数
     * @return
     */
    int count();

    /**
     * 类型
     *
     * @return
     */
    LimitTypeEnum limitType() default LimitTypeEnum.CUSTOMER;

}
