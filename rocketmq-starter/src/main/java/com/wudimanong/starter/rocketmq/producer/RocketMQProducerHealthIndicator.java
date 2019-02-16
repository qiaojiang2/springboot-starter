package com.wudimanong.starter.rocketmq.producer;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.common.ServiceState;
import org.apache.rocketmq.common.help.FAQUrl;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

/**
 * @author Joe
 */
//构建基于SpringBoot的健康检查
public class RocketMQProducerHealthIndicator extends AbstractHealthIndicator {

    private DefaultMQProducer producer;

    public RocketMQProducerHealthIndicator(DefaultMQProducer producer) {
        this.producer = producer;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        if (producer.getDefaultMQProducerImpl().getServiceState() == ServiceState.RUNNING) {
            builder.up();
        } else if ((producer instanceof RocketMQProducer) && !((RocketMQProducer) producer).isEnabled()) {
            builder.up()
                .withDetail("enabled", false);
        } else {
            builder.down(new MQClientException("The producer service state not OK, "//
                + producer.getDefaultMQProducerImpl().getServiceState()//
                + FAQUrl.suggestTodo(FAQUrl.CLIENT_SERVICE_NOT_OK),
                null));
        }
        builder.withDetail("nameSrvAddr", StringUtils.defaultString(producer.getNamesrvAddr(), ""))
            .withDetail("producerGroup", producer.getProducerGroup())
            .withDetail("instanceName", producer.getInstanceName())
            .withDetail("serviceStatus", producer.getDefaultMQProducerImpl().getServiceState())
            .withDetail("clientVersion", MQVersion.getVersionDesc(MQVersion.CURRENT_VERSION));
    }
}
