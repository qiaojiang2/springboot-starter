package com.wudimanong.starter.rocketmq.consumer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/**
 * @author Joe
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RocketMQConsumer {

    /**
     * id用户标示每一个独立的Consumer，配置文件中可以根据id来为每个Consumer增加独立的配置项。 一个应用内id不能相同。
     */
    String id();

    /**
     * consumer所属组，默认为 ${spring.application.name} + "-" + RocketMQConsumer.id() 当同一个应用内两个Consumer需要独立消费同一个topic时需要为两个Consumer分别指定不同的group
     *
     * @return group
     */
    String group() default "";

    /**
     * 订阅的topic
     */
    String topic();

    /**
     * 订阅topic中的tag
     */
    String tags() default "*";
}