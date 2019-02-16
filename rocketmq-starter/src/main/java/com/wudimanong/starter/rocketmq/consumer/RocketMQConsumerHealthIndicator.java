package com.wudimanong.starter.rocketmq.consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.common.ServiceState;
import org.apache.rocketmq.common.help.FAQUrl;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

/**
 * @author Joe
 */
public class RocketMQConsumerHealthIndicator extends AbstractHealthIndicator {

    private RocketMQConsumerRegister register;

    private volatile DefaultMQPushConsumer consumer;

    private RocketMQConsumer annotation;

    public RocketMQConsumerHealthIndicator(RocketMQConsumerRegister register, RocketMQConsumer annotation) {
        this.register = register;
        this.annotation = annotation;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        if (consumer == null) {
            consumer = register.getConsumerMap().get(annotation.id());
        }
        if (consumer == null) {
            return;
        }
        if (consumer.getDefaultMQPushConsumerImpl().getServiceState() == ServiceState.RUNNING) {
            builder.up();
        } else {
            builder.down(new MQClientException("The consumer service state not OK, "//
                + consumer.getDefaultMQPushConsumerImpl().getServiceState()//
                + FAQUrl.suggestTodo(FAQUrl.CLIENT_SERVICE_NOT_OK),
                null));
        }
        builder.withDetail("nameSrvAddr", StringUtils.defaultString(consumer.getNamesrvAddr(), ""))
            .withDetail("consumerGroup", consumer.getConsumerGroup())
            .withDetail("messageModel", consumer.getMessageModel())
            .withDetail("instanceName", consumer.getInstanceName())
            .withDetail("topic", annotation.topic())
            .withDetail("tags", annotation.tags())
            .withDetail("serviceStatus", consumer.getDefaultMQPushConsumerImpl().getServiceState())
            .withDetail("clientVersion", MQVersion.getVersionDesc(MQVersion.CURRENT_VERSION));
    }
}