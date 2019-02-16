package com.wudimanong.starter.rocketmq.producer;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Joe
 */
//封装pub rocketmq的mqtt 消息，提供一致的用法
@Data
@Slf4j
public class MqttProducer {

    public static final String messageTopic = "WUDI_MQTT_MESSAGE_TOPIC";

    private final RocketMQProducer rocketMQProducer;

    @Value("${spring.cloud.client.ipAddress:unknown}")
    private String clientId;

    public MqttProducer(RocketMQProducer rocketMQProducer) {
        this.rocketMQProducer = rocketMQProducer;
    }

    public SendResult send(String topic,
        byte[] payload) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        return send(topic, payload, null);
    }

    public SendResult send(String topic, byte[] payload,
        String keys) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {
        MqttPublishMessage message = new MqttPublishMessage(
            clientId,
            null,
            topic,
            payload
        );

        Message mqttMessage = new Message(messageTopic, JSON.toJSONBytes(message));
        mqttMessage.setTags("Publish");
        if (StringUtils.isNotBlank(keys)) {
            mqttMessage.setKeys(keys);
        }
        return rocketMQProducer.send(mqttMessage);
    }
}
