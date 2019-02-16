package com.wudimanong.starter.rocketmq.producer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.context.SmartLifecycle;
import org.apache.commons.collections4.CollectionUtils;


/**
 * @author Joe
 */
@Slf4j
public class RocketMQProducerLifecycle implements SmartLifecycle {

    @Resource
    private List<RocketMQProducer> mqProducers;

    private AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void start() {
        if (started.compareAndSet(false, true)) {
            if (CollectionUtils.isNotEmpty(mqProducers)) {
                mqProducers.stream()
                    .filter(RocketMQProducer::isEnabled)
                    .forEach(mqProducer -> {
                        try {
                            mqProducer.start();
                            log.info("RocketMQ producer started (group={}, NameSrvAddr={})", mqProducer.getProducerGroup(), mqProducer.getNamesrvAddr());
                        } catch (MQClientException e) {
                            log.error("start RocketMQProducer fail", e);
                        }
                    });
            }
        }
    }

    @Override
    public void stop() {
        if (started.compareAndSet(true, false)) {
            if (CollectionUtils.isNotEmpty(mqProducers)) {
                mqProducers.stream()
                    .filter(RocketMQProducer::isEnabled)
                    .forEach(DefaultMQProducer::shutdown);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return started.get();
    }

    @Override
    public int getPhase() {
        return 0;
    }
}
