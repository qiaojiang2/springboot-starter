package com.wudimanong.starter.rocketmq.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Joe
 */
//MqttPublishMessage 下发给rocketmq的mqtt消息
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttPublishMessage {
    /**
     * 发送这条消息的 clientId
     */
    private String clientId;

    /**
     * 转发这条消息的 brokerId
     */
    private String sourceId;

    /**
     * 这条消息的 topic
     */
    private String topic;

    /**
     * 这条消息的 payload
     */
    private byte[] payload;
}
