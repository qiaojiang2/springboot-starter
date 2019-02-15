package com.wudimanong.starter.rocketmq.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Joe
 */
@Data
@ConfigurationProperties("rocketmq.producer")
public class RocketMQProducerProperties extends RocketMQProperties {
    private boolean enabled = true;

    private String producerGroup;

    private String createTopicKey;

    private int defaultTopicQueueNums = 4;

    private int sendMsgTimeout = 3000;

    private int compressMsgBodyOverHowMuch = 1024 * 4;

    private boolean retryAnotherBrokerWhenNotStoreOK = false;

    private int maxMessageSize = 1024 * 1024 * 4;

    private int checkThreadPoolMinSize = 1;

    private int checkThreadPoolMaxSize = 1;

    private int checkRequestHoldMax = 2000;
}
