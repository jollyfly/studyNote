###1.数据库范式
 1. 第一范式（1NF）：
  >第一范式强调的是列的原子性，即列不能够再分成其他几列。
 2. 第二范式（2NF）：
 >首先要满足第一范式的要求，另外包含两部分内容，一是表必须有主键；
 二是没有包含在主键中的列必须完全依赖于主键，而不能只依赖于主键的一部分。
 例如一个订单明细表（OrderId，ProductId，Quantity，UnitPrice）<br>
 在这张表中，单单通过一个OrderId是不足以成为主键的，主键应该是（OrderId,ProductId）
 可以将表拆分为（OrderId,ProductId,Quantity）和（ProductId,UnitPrice）
 3. 第三范式（3NF）：
  >首先是要满足2NF，另外非主键列必须直接依赖于主键，不能存在传递依赖。即不能存在：非主键列 A 依赖于非主键列 B，非主键列B依赖于主键的情况。<br>
  例如一个订单表【Order】（OrderID，OrderDate，CustomerID，CustomerName，CustomerAddr，CustomerCity）主键是（OrderID）。
  其中 OrderDate，CustomerID，CustomerName，CustomerAddr，CustomerCity 等非主键列都完全依赖于主键（OrderID），所以符合 2NF。不过问题是 CustomerName，CustomerAddr，CustomerCity 直接依赖的是 CustomerID（非主键列），而不是直接依赖于主键，它是通过传递才依赖于主键，所以不符合 3NF。
  通过拆分【Order】为【Order】（OrderID，OrderDate，CustomerID）和【Customer】（CustomerID，CustomerName，CustomerAddr，CustomerCity）从而达到 3NF。

###2.索引基础
####B-Tree索引
>B-Tree索引能够加快访问数据的速度，因为存储引擎不再需要进行全表扫描来获取需要的数据，
取而代之的是从索引的根节点(图示并未画出)开始进行搜索。根节点的槽中存放了指向子节点的指针，
存储引擎根据这些指针向下层查找。通过比较节点页的值和要查找的值可以找到合适的指针进入下层
子节点，这些指针实际上定义了子节点页中值的上限和下限。最终存储引擎要么是找到对应的值，要么
该记录不存在。<br>
索引对多个值进行排序的依据是CREATE TABLE语句中定义索引时列的顺序。
例：假设有如下数据表：
CREATE TABLE People(
	last_name  varchar(50)   not null,
	first_name varchar(50)   not null,
	dob 	   date			 not null,
	gender	   enum('m','f') not null,
	key(last_name, first_name, dob)
);

对于表中的每一行数据，索引中包含了last_name,first_name和dob列的值。
可以使用B-Tree索引的查询类型。B-Tree索引适用于全键值、键值范围或键前缀查找。其中键前缀查找
只适用于根据最左前缀的查找。上面所述的索引对如下类型的查询有效。
 1. 全值匹配<br>
  全值匹配指的是和索引中的所有列进行匹配，例如前面提到的索引可用于查找姓名为 Cuba Allen、
  出生于1960-01-01的人。
 2. 匹配最左前缀(last_name)<br>
  前面提到的索引可用于查找所有姓为Allen的人，即只使用索引的第一列。
 3. 匹配列前缀<br>
 也可以只匹配某一列的值的开头部分。例如前面提到的索引可用于查找所有以J开
 头的姓的人。
这里也只使用了索引的第一列。
 4. 匹配范围值<br>
  例如前面提到的索引可用于查找姓在Allen和Barrymore之间的人，这里也只使
  用了索引的第一列。
 5. 精确匹配某一列并范围匹配另外一列<br>
  前面提到的索引也可用于查找所有姓为Allen，并且名字是字母K开头的人。
 6. 只访问索引的查询<br>
  B-Tree通常可以支持"只访问索引的查询"，即查询只需要访问索引，
  而无须访问数据行。

#####B-Tree索引的限制：
1. 如果不是按照索引的最左列开始查找，则无法使用索引。
2. 不能跳过索引中的列。
3. 如果查询中有某个列的范围查询，则其右边所有列都无法使用索引优化查找。

#####典型优化：
>使用B-Tree存储URL，因为URL一般比较长，因此数据量大的时候，查询会变得缓
 慢。若删除原来URL列上的索引，而新增一个被索引的url_crc列，使用CRC32做
 哈希，就可以使用下面的方式查询：

 ```sql
 #原查询语句：
   SELECT id FROM url WHERE url='http://www.mysql.com';
#修改后的语句：
   SELECT id FROM url WHERE url='http://www.mysql.com'
                 AND url_crc=CRC32('http://www.mysql.com');
 ```
 >这样做的性能会非常高，因为MySQL优化器会使用这个选择性很高而体积很小的基于url_crc列的索引
来完成查找。即使有多个记录有相同的索引值，查找仍然很快，只需要根据哈希值做快速的整数比较就能
找到索引条目，然后一一比较返回对应的行。可以使用触发器来维护哈希值，例：

```sql
#创建表
    CREATE TABLE pseudohash(
        id int unsigned NOT NULL auto_increment,
        url varchar(255) NOT NULL,
        url_crc int unsigned NOT NULL DEFAULT 0,
        PRIMARY KEY(id);
    );
#创建触发器
    CREATE TRIGGER pseudohash_crc_ins BEFORE INSERT ON pseudohash FOR EACH ROW BEGIN
    SET NEW.url_crc=CRC32(NEW.url);
    END;

    CREATE TRIGGER pseudohash_crc_ins BEFORE UPDATE ON pseudohash FOR EACH ROW BEGIN
    SET NEW.url_crc=CRC32(NEW.url);
    END;
```
 #####高性能的索引使用策略
 1. 独立的列
  >一些不恰当的查询会使MySQL无法使用已有的索引，如果查询中的列不是独立的
  列，则MySQL就不会使用索引。“独立的列”是指索引列不能是表达式的一部分，
  也不能是函数的参数。例：
  ```sql
    SELECT actor_id FROM sakila.actor
        WHERE actor_id + 1 = 5;
    ```
  凭肉眼就能看出WHERE中的表达式其实等价于actor_id = 4，但是MySQL无法自
  动解析这个方程式。另一个常见的错误：
```sql
    SELECT ... WHERE TO_DAYS(CURRENT_DATE)
        - TO_DAYS(date_col) <= 10;
```
 2. 前缀索引和索引选择性
  >如果索引列的字符串很长，索引会变得大且慢。通常可以索引开始部分的字符，这样可以大大节约索引
空间，从而提高索引效率。但这样也会降低索引的选择性。对于BLOB、TEXT或者很长的VARCHAR类型的列，
必须使用前缀索引，因为MySQL不允许索引这些列完整的长度。创建索引的SQL:
```sql
    ALTER TABLE sakila.city_demo ADD KEY (city(7));
```
 3. 多列索引
 >很多人对多列索引的理解都不够，一个常见的错误就是，为每个列创建独立的索引，或者按照错误的
顺序创建多列索引。
  - 当出现服务器对多个索引做相交操作时(通常有多个AND条件)，通常意味着需要一个包含所有相关
 列的多列索引，而不是多个独立的单列索引。
  - 当服务器需要对多个索引做联合操作时(通常有多个OR条件)，通常需要消耗大量的CPU和内存资源
 在算法的缓存、排序和合并操作上。特别是当其中有些索引的选择性不高，需要合并扫描返回的大量
 数据的时候。
 4. 重复索引：
 >
  1. 主键列和唯一列，不需要再创建索引，因为MySQL的主键限制和唯一限制都是通过索引来实现的。
  2. 如果创建了索引(A,B),再创建索引(A)就是冗余索引。因为索引(A,B)也可以当作索引(A)来使用。

###3.EXPLAN
>在MySQL中可以使用EXPLAN查看SQL执行计划，用法：
```SQL
    EXPLAIN SELECT * FROM user
```

####3.1结果说明
1. id<br>
    SELECT识别符。这是SELECT查询序列号。这个不重要。
2. select_type<br>
    表示SELECT语句的类型，有以下几种值：
    1. SIMPLE<br>
    表示简单查询，其中不包含连接查询和子查询。
    2. PRIMARY<br>
    表示主查询，或者是最外面的查询语句。
    3. UNION<br>
    表示连接查询的第2个或后面的查询语句。
    4. DEPENDENT UNION<br>
    UNION中的第二个或后面的SELECT语句，取决于外面的查询。
    5. UNION RESULT
    连接查询的结果
    6. SUBQUERY
    子查询中的第1个SELECT语句。
    7. DEPENDENT SUBQUERY
    子查询中的第1个SELECT语句，取决于外面的查询。
    8. DERIVED
    SELECT(FROM 子句的子查询)。
3. type(重要)
    1. system<br>
    表仅有一行，这是const类型的特列，平时不会出现，这个也可以忽略不计。
    2. const<br>
    数据表最多只有一个匹配行，因为只匹配一行数据，所以很快，常用于PRIMARY KEY或者UNIQUE
    索引的查询，可理解为const是最优化的。
    3. eq_ref<br>
    mysql手册是这样说的:"对于每个来自于前面的表的行组合，从该表中读取一行。这可能是最好的
    联接类型，除了const类型。它用在一个索引的所有部分被联接使用并且索引是UNIQUE或PRIMARY KEY"。eq_ref可以用于使用=比较带索引的列。
    4. ref<br>
    查询条件索引既不是UNIQUE也不是PRIMARY KEY的情况。ref可用于=或<或>操作符的带索引的列。
    5. ref_or_null<br>
    该联接类型如同ref，但是添加了MySQL可以专门搜索包含NULL值的行。在解决子查询中经常使用
    该联接类型的优化。<br>
    **上面这五种情况都是很理想的索引使用情况。**<br>
    6. index_merge<br>
    该联接类型表示使用了索引合并优化方法。在这种情况下，key列包含了使用的索引的清单，
    key_len包含了使用的索引的最长的关键元素。
    7. unique_subquery<br>
    该类型替换了下面形式的IN子查询的ref: value IN (SELECT primary_key FROM
    single_table WHERE some_expr)<br>
    unique_subquery是一个索引查找函数,可以完全替换子查询,效率更高。
    8. index_subquery<br>
    该联接类型类似于unique_subquery。可以替换IN子查询,但只适合下列形式的子查询中的非唯一
    索引: value IN (SELECT key_column FROM single_table WHERE some_expr)
    9. range<br>
    只检索给定范围的行,使用一个索引来选择行。
    10. index<br>
    该联接类型与ALL相同,除了只有索引树被扫描。这通常比ALL快,因为索引文件通常比数据文件小。
    11. ALL<br>
    对于每个来自于先前的表的行组合,进行完整的表扫描。（性能最差）
4. table<br>
    输出的行所引用的表。
5. possible_keys<br>
    指出MySQL能使用哪个索引在该表中找到行。如果是空的，没有相关的索引，这时要提高性能，可
    通过校验WHERE子句，看是否引用某些字段，或者检查字段是不是适合索引。
6. key<br>
    显示MySQL实际决定使用的键。如果没有索引被选择，键是NULL。
7. key_len<br>
    显示MySQL决定使用的键的长度。如果键是NULL，长度就是NULL。文档提示特别注意这个值可以得
    出一个多重主键里MySQL实际使用了哪一部分。
8. ref<br>
    显示哪个字段或常数与key一起被使用。
9. rows<br>
    这个数表示MySQL要遍历多少数据才能找到，在innodb上是不准确的。
10. Extra<br>
    该列包含MySQL解决查询的详细信息
    - Distinct：MySQL发现第1个匹配行后,停止为当前的行组合搜索更多的行。
    - Not exists：MySQL能够对查询进行LEFT JOIN优化,发现1个匹配LEFT JOIN标准的行后,不再为前面的的行组合在该表内检查更多的行。
    - range checked for each record (index map: #)：MySQL没有发现好的可以使用的索引,
    但发现如果来自前面的表的列值已知,可能部分索引可以
    使用。
    - Using filesort：MySQL需要额外的一次传递,以找出如何按排序顺序检索行。
    - Using index：从只使用索引树中的信息而不需要进一步搜索读取实际的行来检索表中的列信
    息。
    - Using temporary：为了解决查询,MySQL需要创建一个临时表来容纳结果。
    - Using where：WHERE 子句用于限制哪一个行匹配下一个表或发送到客户。
    - Using sort_union(...), Using union(...), Using intersect(...)：这些
    函数说明如何为index_merge联接类型合并索引扫描。
    - Using index for group-by：类似于访问表的Using index方式,Using index for group-by表示MySQL发现了一个索引,可以用来查 询GROUP BY或DISTINCT查询的所有列,而
    不要额外搜索硬盘访问实际的表。

###4.查询性能优化
>查询的生命周期：从客户端，到服务器，在服务器上进行解析，生成执行计划，执行，返回结果给客户端。
其中”执行“可以认为是最重要的阶段。这其中包括了大量为了检索数据到存储引擎的调用以及调用后的数据
处理，包括排序，分组等。<br>
查询性能低下的最基本原因是访问的数据太多。大部分性能低下的查询都可以通过减少访问的数据量的方式
进行优化。对于低效的查询，我们发现通过下面两个步骤来分析总是很有效：
1. 确认应用程序是否在检索大量超过需要的数据。这通常意味着访问了太多的行，但有时候也可能是访问了
太多的列。
2. 确认MySQL服务器层是否在分析大量超过需要的数据行。

简单的衡量查询开销的三个指标如下：
1. 响应时间
2. 扫描的行数
3. 返回的行数

这三个指标都会记录到MySQL的慢日志中，所以检查慢日志记录是找出扫描行数过多的查询的好办法。
>1. 响应时间<br>
响应时间分为服务时间和排队时间。服务时间是指数据库处理这个查询真正花了多长时间。排队时间
是指服务器因为等待某些资源而没有真正执行查询的时间——可能是等I/O操作完成，也可能是等待行锁，
等待。
2. 扫描的行数和返回的行数<br>
分析查询时，查看该查询扫描的行数是非常有帮助的，这在一定程度上能够说明该查询找到需要的数据
的效率高不高。理想情况下扫描的行数和返回的行数应该是相同的。但实际情况这种事并不多。
3. 扫描的行数和访问类型<br>
在EXPLAIN语句中的type列反应了访问类型，访问类型有很多种，从全表扫描到索引扫描、范围扫描、
唯一索查询、常数引用等。这里列的这些，速度是从慢到快。扫描的行数也是从小到大。需要明白扫描
表，扫描索引，范围访问和单值访问的概念。

一般MySQL能够使用如下三种方式应用WHERE条件，从好到坏依次为：
1. 在索引中使用WHERE条件来过滤不匹配的记录，这是在存储引擎层完成的。
2. 使用索引覆盖扫描(在Extra列中出现了Using index)来返回记录，直接从索引中过滤不需要的记
录并返回命中的结果。这是在Mysql服务器层完成的，但无须再回表查询记录。
3. 从数据表中返回数据，然后过滤不满足条件的记录(在Extra列中出现Using Where)。这在MySQL
服务器层完成，MySQL需要先从数据表读出记录，然后过滤。

如果发现查询需要扫描大量的数据，但只返回少数的行，那么可以尝试下面的方法去优化：
1. 使用索引覆盖扫描，把所有需要用的列都放到索引中，这样存储引擎无须回表获取对应行就可以返
回结果了。
2. 改变库表结构，例如使用单独的汇总表。
3. 重写这个复杂的查询，让MySQL优化器能够以更优化的方式执行这个查询。

在优化有问题的查询时，目标应该是找到一个更优的方法获得实际需要的结果————而不一定总是需要从
MySQL获取一模一样的结果集。有时候，可以将查询转换一种写法让其返回一样的结果，但是性能更好。
但也可以通过修改应用代码，用另一种方式完成查询，最终达到一样的目的。
