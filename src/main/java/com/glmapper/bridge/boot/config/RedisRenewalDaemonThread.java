/*
 * Copyright (c) 2014-2017 Huami, Inc. All Rights Reserved.
 */

package com.glmapper.bridge.boot.config;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @ClassName RedisRenewalDaemonThread
 * @Description Key expired and renewed daemon thread
 * @Author songguolei
 * @Date 2021/7/8 18:47
 * @Version 1.0
 */
public class RedisRenewalDaemonThread extends Thread {

    private final String lockKey;
    private final long expireTime;
    private final RedisTemplate redisTemplate;

    public RedisRenewalDaemonThread(RedisTemplate redisTemplate, String lockKey, long expireTime) {
        this.redisTemplate = redisTemplate;
        this.lockKey = lockKey;
        this.expireTime = expireTime;
    }

    @Override
    public void run() {
        // if the expiration time is very short, less than 3 seconds
        long renew = this.expireTime / 3;
        if (renew == 0) {
            renew = 1L;
        }
        while (true) {
            try {
                // if get null , maybe current lock has been release, so should be return thread
                Object value = redisTemplate.opsForValue().get(lockKey);
                if (value == null) {
                    System.out.println("daemon thread: ending execute.......");
                    break;
                }
                long expire = redisTemplate.getExpire(lockKey);
                // if the remaining expiration time is less than or equal to 1/3, then the renewal.
                // notes: the expire is less than or equal, but if it is less than then the expire may have negative numbers
                if (expire <= renew) {
                    redisTemplate.expire(lockKey, expireTime, TimeUnit.SECONDS);
                }
                // request interval to reduce the frequency of invalid requests
                Thread.sleep(renew * 100);
            } catch (InterruptedException e) {
                //
            }
        }
    }
}