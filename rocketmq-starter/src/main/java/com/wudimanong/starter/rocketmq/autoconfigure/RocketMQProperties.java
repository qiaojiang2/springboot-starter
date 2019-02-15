package com.wudimanong.starter.rocketmq.autoconfigure;

import lombok.Data;

/**
 * @author Joe
 */
@Data
public class RocketMQProperties {
    public static final String DEFAULT_INSTANCE_NAME = "DEFAULT";

    public static final int DEFAULT_CLIENT_CALLBACK_EXECUTOR_THREADS = -1;

    public static final int DEFAULT_POLL_NAME_SERVER_INTERVAL = 30 * 1000;

    public static final int DEFAULT_HEART_BEAT_BROKER_INTERVAL = 30 * 1000;

    public static final int DEFAULT_PERSIST_CONSUMER_OFFSET_INTERVAL = 5 * 1000;

    /**
     * Name Server 地址列表，多个 NameServer 地址用分号 隔开
     */
    private String nameSrvAddr;

    /**
     * 客户端本机 IP 地址，某些机器会发生无法识别客户端 IP 地址情况，需要应用在代码中强制指定
     */
    private String clientIP;

    /**
     * 客户端实例名称，客户端创建的多个 Producer、 Consumer 实际是共用一个内部实例(这个实例包含 网络连接、线程资源等)
     */
    private String instanceName = DEFAULT_INSTANCE_NAME;

    /**
     * 通信层异步回调线程数，默认值-1表示取运行主机的cpu数。
     */
    private int clientCallbackExecutorThreads = DEFAULT_CLIENT_CALLBACK_EXECUTOR_THREADS;

    /**
     * 轮询 Name Server 间隔时间，单位毫秒
     */
    private int pollNameServerInterval = DEFAULT_POLL_NAME_SERVER_INTERVAL;

    /**
     * 向 Broker 发送心跳间隔时间，单位毫秒
     */
    private int heartbeatBrokerInterval = DEFAULT_HEART_BEAT_BROKER_INTERVAL;

    /**
     * 持久化 Consumer 消费进度间隔时间，单位毫秒
     */
    private int persistConsumerOffsetInterval = DEFAULT_PERSIST_CONSUMER_OFFSET_INTERVAL;
}
