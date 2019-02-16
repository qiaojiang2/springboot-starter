package com.wudimanong.starter.rocketmq.producer;

import com.wudimanong.starter.rocketmq.autoconfigure.RocketMQProducerProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.remoting.common.RemotingUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Joe
 */
@Data
@Slf4j
public class RocketMQProducer extends DefaultMQProducer implements InitializingBean {

    private boolean enabled = true;

    private String nameSrvAddr;

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private RocketMQProducerProperties properties;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!enabled) {
            return;
        }
        if (MixAll.DEFAULT_PRODUCER_GROUP.equals(this.getProducerGroup())) {
            this.setProducerGroup(applicationName);
        }

        this.setNamesrvAddr(StringUtils.defaultIfBlank(nameSrvAddr, properties.getNameSrvAddr()));
        if (StringUtils.isEmpty(this.getNamesrvAddr())) {
            this.enabled = false;
            log.info("RocketMQ producer (group = {}) stop create, because nameSrvAddr is unset.", this.getProducerGroup());
            return;
        }

        if (StringUtils.equals(RemotingUtil.getLocalAddress(), this.getClientIP())
            && StringUtils.isNotEmpty(properties.getClientIP())) {
            this.setClientIP(properties.getClientIP());
        }

        log.info("RocketMQ producer created (group={}, NameSrvAddr={})", this.getProducerGroup(), this.getNamesrvAddr());
    }
}