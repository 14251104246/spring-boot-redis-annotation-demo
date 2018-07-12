## spring boot —— redis 缓存注解使用教程

> 示例项目地址：https://github.com/14251104246/spring-boot-redis-annotation-demo

### 依赖
- 在`pom`文件添加如下依赖
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 配置
- 在`application.yml`配置文件添加如下配置
```
spring.cache.type: REDIS

# REDIS (RedisProperties)
spring.redis.database: 0
spring.redis.host: 127.0.0.2
spring.redis.password:
spring.redis.port: 6379
spring.redis.pool.max-idle: 8
spring.redis.pool.min-idle: 0
spring.redis.pool.max-active: 100
spring.redis.pool.max-wait: -1
```

- **在启动类添加`@EnableCaching`注解开启注解驱动的缓存管理**，如下
```
@Configuration
@EnableAutoConfiguration
@ComponentScan("org.hsweb.demo")
@MapperScan("org.hsweb.demo.dao")
@EnableCaching//开启注解驱动的缓存管理
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```
### 示例项目基本逻辑
- 为了方便理解，示例项目演示了一个简单的对用户数据的增删改查，主要有两方面：
    - redis缓存注解与mybatis集成，主要逻辑位于`SimpleUserService`
    - 仅使用redis缓存注解，主要逻辑位于`RedisOnlyServiceImpl`

- 示例java代码结构
```
.
├── Application.java    //启动类
├── controller 
│   ├── RedisOnlyController.java
│   └── UserController.java
├── dao
│   └── UserDao.java    //mapper接口（mybatis）
├── po
│   └── User.java   // 用户po类
└── service
    ├── impl
    │   ├── RedisOnlyServiceImpl.java
    │   └── SimpleUserService.java
    ├── RedisOnlyService.java
    └── UserService.java
```

### redis 注解使用入门

#### `@Cacheable` 注解简单使用教程——用于查询操作接口

- `@Cacheable`注解的作用：缓存被调用方法的结果（返回值），已经缓存就不再调用注解修饰的方法，适用于查询接口

- `controller`层和`service`层的相关代码如下
```
@RequestMapping("/redisOnly")
@RestController()
public class RedisOnlyController {
    @Resource
    RedisOnlyService redisOnlyService;
    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
    public User getById(@PathVariable("id") String id) {
        return redisOnlyService.selectById(id);
    }
}

@Service
public class RedisOnlyServiceImpl implements UserService {
    /**
     *  先用id生成key，在用这个key查询redis中有无缓存到对应的值
     *
     *  若无缓存，则执行方法selectById，并把方法返回的值缓存到redis
     *
     *  若有缓存，则直接把redis缓存的值返回给用户，不执行方法
     */
    @Cacheable(cacheNames="user", key="#id")
    @Override
    public User selectById(String id) {
        //直接new一个给定id的用户对象，来返回给用户
        return new User(id,"redisOnly","password");
    }
}
```
- 可见这是一个简单查询用户接口。它与典型接口只多了`@Cacheable`注解
    - 为了方便演示，我直接返回一个用户对象，而不是查询数据

- 启动程序，访问`http://localhost:9999/redisOnly/user/9876`接口，查询一个id为`9876`的用户，结果如图：


> ![image.png](https://upload-images.jianshu.io/upload_images/7176877-c793f3097af29239.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

- 可以看见返回用户数据的同时，spring还把用户数据缓存到redis中。

- 第二次访问`http://localhost:9999/redisOnly/user/9876`接口（查询id为`9876`的用户）时就会直接从redis中返回redis缓存的用户数据
    - 而不再执行`RedisOnlyServiceImpl`的`selectById()`方法


#### 深入理解`@Cacheable`注解

- `@Cacheable`注解的作用：缓存被调用方法的结果（返回值），已经缓存就不再调用注解修饰的方法，适用于查询接口

- 以下面代码的`@Cacheable(cacheNames="user", key="#id")`为例说明
    
    - `cacheNames="user"`用于把用户数据存在同一个用户空间`user`中，如图
    
        > ![image.png](https://upload-images.jianshu.io/upload_images/7176877-88d6ec4ff9b39a2b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
        
        - `cacheNames="user"`本质是在redis缓存键值对中的`key`加个`user：`的前缀
    - `key="#id"`当中的`#id`是按照spring表达式（[详细看官方教程](https://docs.spring.io/spring/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html#expressions-ref-variables)）书写的,这里意思是使用方法参数中的`id`参数生成缓存键值对中的`key`
        - 若不满足于上面的key生成规则，可以通过实现`KeyGenerator`接口自定义，详细看
    - 实质上`cacheNames`和`key`这连个属性都是用于配置redis缓存键值对中的`key`
    - 这里仅解析个属性的作用，下面在解析spring遇到这个注解后的逻辑

- 为了方便理解，列出示例代码的逻辑


```
graph TB
    A[查询id为9876的用户] -->|RedisOnlyController| B(调用RedisOnlyServiceImpl前被aop拦截)
    B --> C{id为9876的用户数据是否缓存到redis?}
    C -->|是| D[从redis中获取用户数据并立刻返回]
    C -->|否| E[执行RedisOnlyServiceImpl的selectById方法并缓存结果]
```


- 上面逻辑都是通过aop封装好的，对我们来说上面过程是透明的。详细可以看源码中`CacheAspectSupport`类的第二个`execute()`方法（[传送门](https://github.com/ndimiduk/spring-framework/blob/64e0b1dc25773bbf344a8a15c35ee6fb8d06cf2a/org.springframework.context/src/main/java/org/springframework/cache/interceptor/CacheAspectSupport.java)）

#### `@CacheEvict` 注解简单使用教程——用于删除操作接口

- `@CacheEvict`注解的作用：删除redis中对应的缓存，适用于删除接口

- `controller`层和`service`层的相关代码如下
```
@RequestMapping("/redisOnly")
@RestController()
public class RedisOnlyController {
    @RequestMapping(value = "/user/{id}", method = RequestMethod.DELETE)
    public boolean delete(@PathVariable String id) {
        return redisOnlyService.delete(id);
    }
}

@Service
public class RedisOnlyServiceImpl implements UserService {
    @CacheEvict(cacheNames="user", key="#id")
    @Override
    public boolean delete(String id) {
        // 可以在这里添加删除数据库对应用户数据的操作
        return true;
    }
}
```
- service中的`delete()`是一个空方法，仅多了个`@CacheEvict`



- 启动程序，设置请求方法为`DELETE`，用postman访问`http://localhost:9999/redisOnly/user/9876`接口，即可删除id为`9876`的用户数据于redis的缓存
     - 注意：`@RequestMapping`设置的请求方法为`RequestMethod.DELETE`

- `@CacheEvict(cacheNames="user", key="#id")`注解中的两个属性类似上面说的`@Cacheable(cacheNames="user", key="#id")`
    

- 原理很简单，仅仅是把缓存数据删除，无特别逻辑

- 若想删除redis缓存的所有用户数据，可以把注解改成`@CacheEvict(cacheNames="user", allEntries=true)`
    - 本质是删除redis数据库的`user`命名空间下的所有键值对

#### `@CachePut` 注解简单使用教程——用于删除操作接口

- `@CachePut`注解的作用同样是缓存被调用方法的结果（返回值），当与`@Cacheable`不一样的是：
    - `@CachePut`在值已经被缓存的情况下仍然会执行被`@CachePut`注解修饰的方法，而`@Cacheable`不会
    - `@CachePut`注解适用于**更新操作**和**插入操作**

- 我们从更新操作（`update`方法)了解`@CachePut`注解
- `controller`层和`service`层的相关代码如下
```
@RequestMapping("/redisOnly")
@RestController()
public class RedisOnlyController {
    @Resource
    RedisOnlyService redisOnlyService;

    @RequestMapping(value = "/user/{id}", method = RequestMethod.PUT)
    public User update(@PathVariable String id, @RequestBody User user) {
        user.setId(id);
        redisOnlyService.update(user);
        return user;
    }
}

@Service
public class RedisOnlyServiceImpl implements UserService {
    // 记录
    private AtomicInteger executeCout = new AtomicInteger(0);

    @CachePut(cacheNames="user", key="#user.id")
    @Override
    public User update(User user) {
        // 每次方法执行executeCout
        user.setUsername("redisOnly" + executeCout.incrementAndGet());
        // 必须把更新后的用户数据返回，这样才能把它缓存到redis中
        return user;
    }
}
```
- 注意: 由于`update(User user)`方法的参数不再是id而是对象user，`key="#id"`中的SpEL（spring表达式)被改为`#user.id`

- 启动程序，设置请求方法为`DELETE`，用postman访问`http://localhost:9999/redisOnly/user/9876`接口5次，如下图
    - 可以看到每次访问接口时时`update()`方法都会执行一次(即，`executeCout`的值为5)
    - 注意：`@RequestMapping`设置的请求方法为`RequestMethod.DELETE`

> ![image.png](https://upload-images.jianshu.io/upload_images/7176877-11ea2fbb72991198.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

- 为了更好地对比，我把`@Cacheable`注解修饰的查询操作改成如下，同样用postman访问5次
```
@Cacheable(cacheNames="user", key="#id")
@Override
public User selectById(String id) {
    return new User(id,"redisOnly" + executeCout.incrementAndGet(),"password");
}
```
- 测试结果是，`selectById()`方法只执行了一次，后面4次请求都是从redis缓存里取出用户数据返回到客户端


### redis注解与mybatis一起使用

- redis注解与mybatis一起使用的方式很简单，就是在上面提到的各个方法内添加相应的dao操作就行了

- 具体的service层代码如下：
```
@Service("userService")
public class SimpleUserService implements UserService {
    @Resource
    private UserDao userDao;
    @Cacheable(cacheNames="user", key="#id")
    @Override
    public User selectById(String id) {
        return userDao.selectById(id);
    }
    @Cacheable(cacheNames="user", key="#username")
    @Override
    public User selectByUserName(String username) {
        return userDao.selectByUserName(username);
    }
    @Override
    public List<User> selectAll() {
        return userDao.selectAll();
    }
    @CachePut(cacheNames="user", key="#user.id")
    @Override
    public User insert(User user) {
        user.setId(UUID.randomUUID().toString());
        userDao.insert(user);
        return user;
    }
    @CachePut(cacheNames="user", key="#user.id")
    @Override
    public User update(User user) {
        userDao.update(user);
        return user;
    }
    @CacheEvict(cacheNames="user", key="#id")
    @Override
    public boolean delete(String id) {
        return userDao.deleteById(id) == 1;
    }
}
```

### redis 注解的其他知识点

- 除了`cacheNames`和`key`，其他注解属性的作用
    - `keyGenerator`：定义键值对中key生成的类，和`key`属性的不能同时存在
    - `sync`：如果设置sync=true：
        - a. 如果缓存中没有数据，多个线程同时访问这个方法，则只有一个方法会执行到方法，其它方法需要等待; 
        - b. 如果缓存中已经有数据，则多个线程可以同时从缓存中获取数据;
    - `condition`: 在执行方法后，如果condition的值为true，则缓存数据；如果不满足条件，仅执行方法，不缓存结果
    - `unless` ：在执行方法后，判断unless ，如果值为true，则不缓存数据
- `@CacheConfig`: 类级别的注解：
    - 如果我们在此注解中定义`cacheNames`，则此类中的所有方法上的`cacheNames`默认都是此值。
    - 当然`@Cacheable`也可以重定义`cacheNames`的值