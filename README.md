# Getting Started

本 guides 基于 SpringBoot + Spring Data Redis, 实现了两个基本能力：

* redis key 超时回调
* 基于 redis 实现的分布式锁实现

redis 的键空间通知事件在客户端多实例集群部署的情况下会被所有实例监听，若不加处理就会出现重复消费的情况，此场景在实际的生产中是不允许存在的。
因此在处理消息方法里需要使用分布式锁，防止同一个 KEY 被多个实例监听，避免重复处理任务。


## redis key 超时回调

基于 [KeyExpirationEventMessageListener](https://docs.spring.io/spring-data/data-redis/docs/current/api/org/springframework/data/redis/listener/KeyExpirationEventMessageListener.html)

samples from https://www.javatips.net/api/spring-data-redis-master/src/main/java/org/springframework/data/redis/listener/KeyExpirationEventMessageListener.java;

## 分布式锁

```
加锁
 1. 通过 setnx 向特定的 key 写入当前线程ID，并设置失效时间，写入成功即加锁成功
 2. 注意点：
    - 必须给锁设置一个失效时间            ----->    避免死锁
    - 加锁时，指定当前线程ID             ----->    避免锁误删
    - 写入与设置失效时间必须是同时         ----->    保证加锁的原子性
 使用：
   SET_NX & expire

解锁：
	if redis.call('get', KEYS[1]) == ARGV[1] then
        return redis.call('del', KEYS[1])
    else
        return 0
    end
		
``` 