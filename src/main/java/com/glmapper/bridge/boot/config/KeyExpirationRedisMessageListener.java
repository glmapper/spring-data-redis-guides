/*
 * Copyright (c) 2014-2017 Huami, Inc. All Rights Reserved.
 */

package com.glmapper.bridge.boot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * @ClassName RedisMessageListener
 * @Description TODO
 * @Author songguolei
 * @Date 2021/7/8 17:03
 * @Version 1.0
 */
@Component
public class KeyExpirationRedisMessageListener extends KeyExpirationEventMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyExpirationRedisMessageListener.class);

    // Topic
    private static final Topic KEY_EVENT_EXPIRED_TOPIC = new PatternTopic("__keyevent@*__:expired");

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * inject RedisMessageListenerContainer
     *
     * @param container
     */
    public KeyExpirationRedisMessageListener(RedisMessageListenerContainer container) {
        super(container);
    }

    @Override
    public void doRegister(RedisMessageListenerContainer container) {
        container.addMessageListener(this, KEY_EVENT_EXPIRED_TOPIC);
    }

    /**
     * handler message
     * @param message
     */
    @Override
    public void doHandleMessage(Message message) {
        String key = new String(message.getBody(), StandardCharsets.UTF_8);
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        LOGGER.info("callback redis key expiration, channel: {}, key: {}.", channel, key);
        boolean lockSuccess = redisDistributedLock.tryToLock(key, String.valueOf(Thread.currentThread().getId()), 10);
        if (lockSuccess) {
            System.out.println("[redis message listener]-------- lock success --------");
            System.out.println("[redis message listener] -- key: " + key);
            // note: 这里返回的 keyResult 应该是 null, 因为已经过期了
            String keyResult = redisTemplate.opsForValue().get(key);
            System.out.println("[redis message listener] -- keyResult: " + keyResult);

            // todo 这里拿到过期的数据，然后将对应表中的数据删除掉
            // doSomething()

            // 释放锁，如果锁到了超时时间，没有续期，也就会自动释放
            boolean releaseLockSuccess = redisDistributedLock.releaseLock(key, String.valueOf(Thread.currentThread().getId()));
            if (releaseLockSuccess) {
                System.out.println("[redis message listener]-------- release lock success --------");
            }
        }
    }
}