package com.chao.redis;

import redis.clients.jedis.Jedis;

/**
 * Created by wanzhichao on 2017/6/9.
 */
public class RedisDemo {
    public static void main(String[] args) {
        Jedis jedis = new Jedis("127.0.0.1",6379);
        jedis.set("content","hello");
//        String content = jedis.get("content");
//        System.out.println(content);
//
//        String books[] = {"aaa","bbb","ccc","ddd","eee"};
//
//        jedis.lpush("books",books);
//        System.out.println(jedis.llen("books"));
//        jedis.lrange("books",0,100).forEach(e->{
//            System.out.println(e);
//        });
//        System.out.println("LINDEX:"+jedis.lindex("books",3));
//        jedis.linsert("books", BinaryClient.LIST_POSITION.AFTER,"aaa","xxx");
//        System.out.println("-----------");
//        jedis.lrange("books",0,100).forEach(e->{
//            System.out.println(e);
//        });
//        System.out.println("-----------");

        jedis.lrem("books",0,"ccc");

        jedis.lset("books",0,"yy");
        jedis.lrange("books",0,100).forEach(e->{
            System.out.println(e);
        });
        jedis.close();
    }
}
