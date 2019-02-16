package com.wudimanong.starter.rocketmq.autoconfigure;

import com.wudimanong.starter.rocketmq.consumer.RocketMQConsumer;
import com.wudimanong.starter.rocketmq.consumer.RocketMQConsumerHealthIndicator;
import com.wudimanong.starter.rocketmq.consumer.RocketMQConsumerRegister;
import com.wudimanong.starter.rocketmq.producer.RocketMQProducer;
import com.wudimanong.starter.rocketmq.producer.RocketMQProducerHealthIndicator;
import com.wudimanong.starter.rocketmq.producer.RocketMQProducerLifecycle;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.rocketmq.client.consumer.listener.MessageListener;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * @author Joe
 */
@Configuration
@EnableConfigurationProperties({
    RocketMQProducerProperties.class,
    RocketMQConsumerProperties.class,
})
@AutoConfigureOrder
public class RocketMQAutoConfiguration {
    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    @ConditionalOnProperty(value = "rocketmq.producer.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(RocketMQProducer.class)
    public RocketMQProducer mqProducer() {
        return new RocketMQProducer();
    }

    @Bean
    @ConditionalOnBean(DefaultMQProducer.class)
    public RocketMQProducerLifecycle rocketMQLifecycle() {
        return new RocketMQProducerLifecycle();
    }

    @Bean
    @ConditionalOnBean(annotation = RocketMQConsumer.class)
    @ConditionalOnProperty(value = "rocketmq.consumer.enabled", havingValue = "true", matchIfMissing = true)
    public RocketMQConsumerRegister rocketMQConsumerRegister() {
        return new RocketMQConsumerRegister();
    }

    @Autowired
    private HealthAggregator healthAggregator;

    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnBean(DefaultMQProducer.class)
    @ConditionalOnMissingBean(name = "rocketMQProducerHealthIndicator")
    public HealthIndicator rocketMQProducerHealthIndicator(Map<String, DefaultMQProducer> producers) {
        if (producers.size() == 1) {
            return new RocketMQProducerHealthIndicator(producers.values().iterator().next());
        }
        CompositeHealthIndicator producersComposite = new CompositeHealthIndicator(this.healthAggregator);
        for (Map.Entry<String, DefaultMQProducer> entry : producers.entrySet()) {
            producersComposite.addHealthIndicator(entry.getKey(),
                new RocketMQProducerHealthIndicator(entry.getValue()));
        }
        return producersComposite;
    }

    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnBean(RocketMQConsumerRegister.class)
    @ConditionalOnMissingBean(name = "rocketMQConsumerHealthIndicator")
    public HealthIndicator rocketMQConsumerHealthIndicator(ApplicationContext applicationContext,
        RocketMQConsumerRegister register) {

        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(RocketMQConsumer.class);

        List<RocketMQConsumer> anns = beans.values().stream()
            .filter(b -> b instanceof MessageListener)
            .map(AopUtils::getTargetClass)
            .map(c -> AnnotationUtils.findAnnotation(c, RocketMQConsumer.class)).collect(Collectors.toList());
        if (anns.size() == 1) {
            return new RocketMQConsumerHealthIndicator(register, anns.get(0));
        }
        CompositeHealthIndicator consumersComposite = new CompositeHealthIndicator(this.healthAggregator);
        anns.forEach(ann -> {
            consumersComposite.addHealthIndicator(ann.id(),
                new RocketMQConsumerHealthIndicator(register, ann));
        });
        return consumersComposite;
    }
}
