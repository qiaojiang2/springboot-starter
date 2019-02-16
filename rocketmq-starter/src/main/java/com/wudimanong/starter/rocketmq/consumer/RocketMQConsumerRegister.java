package com.wudimanong.starter.rocketmq.consumer;

import com.wudimanong.starter.rocketmq.autoconfigure.RocketMQConsumerProperties;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;

/**
 * @author Joe
 */
@Slf4j
public class RocketMQConsumerRegister implements ApplicationContextAware, EnvironmentAware, SmartLifecycle, ApplicationListener<ApplicationReadyEvent> {

    private AtomicBoolean started = new AtomicBoolean(false);

    private AtomicBoolean initialized = new AtomicBoolean(false);

    private ApplicationContext applicationContext;

    private Environment environment;

    @Autowired
    private RocketMQConsumerProperties defaultConsumerProperties;

    private Map<DefaultMQPushConsumer, RocketMQConsumer> consumerAnnoMap = new ConcurrentHashMap<>();

    private Map<String, DefaultMQPushConsumer> consumerMap = new ConcurrentHashMap<>();

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
        if (initialized.compareAndSet(false, true)) {
            findAndSubscribeListener();
        }
    }

    private void startConsumers() {
        for (Map.Entry<String, DefaultMQPushConsumer> entry : consumerMap.entrySet()) {
            try {
                RocketMQConsumer ann = consumerAnnoMap.get(entry.getValue());
                DefaultMQPushConsumer consumer = entry.getValue();
                entry.getValue().start();

                log.info("RocketMQ consumer started (id = {}, group={}, NameSrvAddr={}, topic={}, tags={})",
                    ann.id(), consumer.getConsumerGroup(), consumer.getNamesrvAddr(), ann.topic(), ann.tags());
            } catch (MQClientException e) {
                log.error("RocketMQ start fail, id = " + entry.getKey(), e);
                throw new IllegalStateException("RocketMQ start fail, id = " + entry.getKey(), e);
            }
        }
    }

    private void findAndSubscribeListener() {
        Map<String, Object> listeners = applicationContext.getBeansWithAnnotation(RocketMQConsumer.class);
        listeners.forEach((name, bean) -> {
            if (!(bean instanceof MessageListener)) {
                throw new UnsupportedOperationException("@RocketMQConsumer only support MessageListenerConcurrently or MessageListenerOrderly");
            }
            Class<?> clazz = AopUtils.getTargetClass(bean);
            RocketMQConsumer consumerAnno = AnnotationUtils.findAnnotation(clazz, RocketMQConsumer.class);
            if (consumerMap.containsKey(consumerAnno.id())) {
                throw new IllegalArgumentException("RocketMQConsumer id duplicate, id = " + consumerAnno.id());
            }
            createConsumer(consumerAnno, (MessageListener) bean);
        });
    }

    private void createConsumer(RocketMQConsumer consumerAnno, MessageListener listener) {
        boolean enabled = environment.getProperty("rocketmq." + consumerAnno.id() + ".consumer.enabled", Boolean.class, true);
        if (!enabled) {
            log.info("RocketMQ Consumer ({}) is disabled.", consumerAnno.id());
            return;
        }

        //groupName 优先级：@RocketMQConsumer > rocketmq.{id}.consumer.consumerGroup >
        // rocketmq.consumer.consumerGroup > spring.application.name + "-" + id
        String groupName = StringUtils.defaultIfBlank(consumerAnno.group(),
            environment.getProperty("rocketmq." + consumerAnno.id() + ".consumer.consumerGroup",
                StringUtils.defaultIfBlank(defaultConsumerProperties.getConsumerGroup(),
                    environment.getProperty("spring.application.name") + "-" + consumerAnno.id())));
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(groupName);

        fillConsumerProperties(consumer, consumerAnno);

        try {
            consumer.subscribe(consumerAnno.topic(), consumerAnno.tags());
        } catch (MQClientException e) {
            log.error("RocketMQ subscribe fail, id = {}, topic = {}, tags = {}", consumerAnno.id(), consumerAnno.topic(),
                consumerAnno.tags(), e);
            throw new IllegalStateException("RocketMQ subscribe fail", e);
        }

        if (listener instanceof MessageListenerConcurrently) {
            consumer.registerMessageListener((MessageListenerConcurrently) listener);
        } else {
            consumer.registerMessageListener((MessageListenerOrderly) listener);
        }

        log.info("RocketMQ consumer created (id = {}, group={}, NameSrvAddr={}, topic={}, tags={}, listener={})",
            consumerAnno.id(), consumer.getConsumerGroup(), consumer.getNamesrvAddr(), consumerAnno.topic(),
            consumerAnno.tags(), listener.getClass().getSimpleName());
        consumerAnnoMap.put(consumer, consumerAnno);
        consumerMap.put(consumerAnno.id(), consumer);
    }

    private void fillConsumerProperties(DefaultMQPushConsumer consumer, RocketMQConsumer consumerAnno) {
        String rootKey = "rocketmq." + consumerAnno.id() + ".consumer.";

        consumer.setNamesrvAddr(environment.getProperty(rootKey + "nameSrvAddr",
            defaultConsumerProperties.getNameSrvAddr()));

        consumer.setClientIP(environment.getProperty(rootKey + "clientIP",
            defaultConsumerProperties.getClientIP()));

        consumer.setInstanceName(environment.getProperty(rootKey + "instanceName",
            defaultConsumerProperties.getInstanceName()));

        consumer.setClientCallbackExecutorThreads(environment.getProperty(rootKey + "clientCallbackExecutorThreads", Integer.class,
            defaultConsumerProperties.getClientCallbackExecutorThreads()));
        
        consumer.setPollNameServerInterval(environment.getProperty(rootKey + "pollNameServerInterval", Integer.class,
            defaultConsumerProperties.getPollNameServerInterval()));

        consumer.setHeartbeatBrokerInterval(environment.getProperty(rootKey + "heartbeatBrokerInterval", Integer.class,
            defaultConsumerProperties.getHeartbeatBrokerInterval()));

        consumer.setPersistConsumerOffsetInterval(environment.getProperty(rootKey + "persistConsumerOffsetInterval", Integer.class,
            defaultConsumerProperties.getPersistConsumerOffsetInterval()));

        String customValue = environment.getProperty(rootKey + "consumeFromWhere");
        consumer.setConsumeFromWhere(StringUtils.isBlank(customValue) ?
            defaultConsumerProperties.getConsumeFromWhere() : ConsumeFromWhere.valueOf(customValue));

        customValue = environment.getProperty(rootKey + "messageModel");
        consumer.setMessageModel(StringUtils.isBlank(customValue) ?
            defaultConsumerProperties.getMessageModel() : MessageModel.valueOf(customValue));

        consumer.setConsumeThreadMin(environment.getProperty(rootKey + "consumeThreadMin", Integer.class,
            defaultConsumerProperties.getConsumeThreadMin()));

        consumer.setConsumeThreadMax(environment.getProperty(rootKey + "consumeThreadMax", Integer.class,
            defaultConsumerProperties.getConsumeThreadMax()));

        consumer.setConsumeConcurrentlyMaxSpan(environment.getProperty(rootKey + "consumeConcurrentlyMaxSpan", Integer.class,
            defaultConsumerProperties.getConsumeConcurrentlyMaxSpan()));

        consumer.setPullThresholdForQueue(environment.getProperty(rootKey + "pullThresholdForQueue", Integer.class,
            defaultConsumerProperties.getPullThresholdForQueue()));

        consumer.setPullInterval(environment.getProperty(rootKey + "pullInterval", Integer.class,
            defaultConsumerProperties.getPullInterval()));

        consumer.setConsumeMessageBatchMaxSize(environment.getProperty(rootKey + "consumeMessageBatchMaxSize", Integer.class,
            defaultConsumerProperties.getConsumeMessageBatchMaxSize()));

        consumer.setPullBatchSize(environment.getProperty(rootKey + "pullBatchSize", Integer.class,
            defaultConsumerProperties.getPullBatchSize()));

    }

    @Override
    public void stop() {
        if (started.compareAndSet(true, false)) {
            consumerMap.values().forEach(DefaultMQPushConsumer::shutdown);
        }
    }

    @Override
    public boolean isRunning() {
        return initialized.get() && started.get();
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (event.getApplicationContext() != applicationContext) {
            //当系统里有多个SpringApplication时会导致有多个AppliationReadyEvent事件，这时候只接受来自直接持有Bean的Application发出的事件。
            //例如spring-cloud-stream里会额外创建一个SpringApplication用于管理依赖配置。
            return;
        }
        if (initialized.get() && started.compareAndSet(false, true)) {
            startConsumers();
        }
    }

    public Map<DefaultMQPushConsumer, RocketMQConsumer> getConsumerAnnoMap() {
        return consumerAnnoMap;
    }

    public Map<String, DefaultMQPushConsumer> getConsumerMap() {
        return consumerMap;
    }
}