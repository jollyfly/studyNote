###1.Redis的基本数据类型
>redis中目前支持五种数据类型：
1.  String 
2.  List 
3.  Hash 
4.  Set
5.  Sorted Set
####1.1.String
#####1.1.1.结构说明
>String是简单的key-value键值对。
#####1.1.2基本命令
>赋值与取值：<br>
SET key value
GET key
```
redis 127.0.0.1:6379> set content hello
OK
redis 127.0.0.1:6379> get content
"hello"
```
#####1.1.3.Jedis中操作String
```java
public class RedisDemo{
    public static void main(String... args){
        Jedis jedis = new Jedis("127.0.0.1",6379);
        String content = jedis.get("content");
        System.out.println(content);
        jedis.close();
    }
} 
```
####1.2.List
#####1.2.1.结构说明
>redis中的List实现为一个双向链表，类似于java中的LinkedList。支持反向查找和遍历。
#####1.2.2基本命令
- BLPOP
    >BLPOP key1 [key2] timeout 取出并获取列表中的第一个元素，或阻塞，直到有可用
- BRPOP
    >BRPOP key1 [key2] timeout 取出并获取列表中的最后一个元素，或阻塞，直到有可用
- LINDEX
    >LINDEX key index 根据一个列表的索引，获取对应位置的元素
- LINSERT
    >LINSERT key BEFORE|AFTER pivot value 在列表中的指定元素之后或之前插入一个元素，若
    有重复元素仅对第一个生效。
- LLEN
    >LLEN key 获取列表的长度
- LRANGE
    >LRANGE key start stop 从一个列表中获取各种元素
- LREM
    >LREM key count value 从列表中删除第count个指定value。如果count是0，所有的值为value
    的元素都会被删除。如果count是一个负数，则从列表的尾部到头部删除。
- LSET
    >LSET key index value 将列表中指定索引的值更新为value。
#####1.2.3基本命令使用
```
redis 127.0.0.1:6379> lpush list1 redis
(integer) 1
redis 127.0.0.1:6379> lpush list1 hello
(integer) 2
redis 127.0.0.1:6379> rpush list1 world
(integer) 3
redis 127.0.0.1:6379> llen list1
(integer) 3
redis 127.0.0.1:6379> lrange list1 0 3
1) "hello"
2) "redis"
3) "world"
redis 127.0.0.1:6379> lpop list1
"hello"
redis 127.0.0.1:6379> rpop list1
"world"
redis 127.0.0.1:6379> lrange list1 0 3
1) "redis"
```
#####1.2.4jedis中操作List
```
    String books[] = {"aaa","bbb","ccc","ddd","eee"};
    jedis.lpush("books",books);
    System.out.println(jedis.llen("books"));
    jedis.lrange("books",0,100).forEach(e->{
        System.out.println(e);
    });
    System.out.println("LINDEX:"+jedis.lindex("books",3));
    jedis.linsert("books", BinaryClient.LIST_POSITION.AFTER,"aaa","xxx");
    System.out.println("-----------");
    jedis.lrange("books",0,100).forEach(e->{
        System.out.println(e);
    });
    System.out.println("-----------");

    jedis.lrem("books",0,"ccc");

    jedis.lset("books",0,"yy");
    jedis.lrange("books",0,100).forEach(e->{
        System.out.println(e);
    });
    jedis.close();
    
```
####1.3.Hash
#####1.3.1结构说明
>散列类型存储了字段和字段值的映射，但字段值只能是字符串，不支持其他类型，也就是说，散列类型不能
嵌套其他的数据类型。
#####1.3.2基本命令使用
- HSET 
  >HSET key field value 设置对象指定字段的值
- HGET
  >HGET key field 获取对象中该field属性域的值
- HMSET
  >HMSET key field value [field value ...] 同时设置对象中一个或多个字段的值
- HMGET
  >HMGET key field[field...] 获取对象的一个或多个指定字段的值
- HGETALL
  >HGETALL key 获取对象的所有属性域和值
- HDEL
  >HDEL key field[field...] 删除对象的一个或几个属性域，不存在的属性将被忽略
#####1.3.3使用示例
```
127.0.0.1:6379> hset person name jack
(integer) 1
127.0.0.1:6379> hset person age 20
(integer) 1
127.0.0.1:6379> hset person sex famale
(integer) 1
127.0.0.1:6379> hgetall person
1) "name"
2) "jack"
3) "age"
4) "20"
5) "sex"
6) "famale"
127.0.0.1:6379> hkeys person
1) "name"
2) "age"
3) "sex"
127.0.0.1:6379> hvals person
1) "jack"
2) "20"
3) "famale"
```
#####1.3.4.Jedis中操作Hash结构
```
   Map<String,String> dataMap = new HashMap<String,String>();
   dataMap.put("id","1001");
   dataMap.put("name","Lisa");
   dataMap.put("age","23");
   jedis.hmset("user_1001",dataMap);
   System.out.println(jedis.hget("user_1001","age"));
   dataMap = jedis.hgetAll("user_1001");
   System.out.println(dataMap);
```
###2.Redis配置文件
>redis默认配置文件为redis.conf,常用配置如下：
1. port  服务端口号
2. bind  绑定ip，其他的ip不能访问（多个ip用空格隔开）
3. databases 数据库数量，默认为16个
4. daemonize 设置为守护进程
5. maxmemory 最大内存大小

###3.Redis的持久化
>Redis支持两种方式的持久化，一种是RDB，一种是AOF。可以单独使用其中一种或者两种结合使用。
####3.1.RDB持久化
>RDB是用过快照完成的，当符合一定条件时，Redis会自动将内存中的所有数据进行快照并且存储到硬盘上。
进行快照的条件在配置文件中指定。RDB是Redis默认的持久化方式。<br>
在配置文件中预置了3个RDB持久化的条件：
```
save 900 1 #15分钟内有至少一个键被更改则进行快照
save 300 10 #5分钟内至少有10个键被更改则进行快照
save 60 100000 #1分钟内至少有10000个键被更改则进行快照
```
>以上条件之间是或的关系。默认的rdb文件路径是在当前目录，文件名是：dump.rdb，可以在配置文件
中修改路径和文件名，分别是dir和dbfilename。注释掉所有的RDB触发条件即可关闭RDB持久化。
####3.2.AOF持久化
>Redis的AOF持久化的原理是将发送到Redis服务端的每一条命令都记录下来，并且保存到硬盘中的AOF文件。
AOF文件的位置和RDB文件的位置相同，都是通过dir参数设置。默认的文件名是appendonly.aof，可以通过
appendfilename参数修改。AOF持久化默认是不开启的，在配置文件中开启：
```appendonly  yes```<br>
###4.Redis集群
>在Redis3.0及以上版本一大特性就是集群。

