package org.hsweb.demo.service.impl;

import org.hsweb.demo.po.User;
import org.hsweb.demo.service.RedisOnlyService;
import org.hsweb.demo.service.UserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service("redisOnlyService")
public class RedisOnlyServiceImpl implements UserService {
    private AtomicInteger executeCout = new AtomicInteger(0);

    /**
     *  先用id生成key，在用这个key查询redis中有无缓存到对应的值
     *
     *  若无缓存，则执行方法selectById，并把方法返回的值缓存到redis
     *
     *  若有缓存，则直接把redis缓存的值返回给用户，不执行方法
     */
//    @Cacheable(cacheNames="user", key="#id")
//    @Override
//    public User selectById(String id) {
//        return new User(id,"redisOnly","password");
//    }
    //改造后的查询接口
    @Cacheable(cacheNames="user", key="#id")
    @Override
    public User selectById(String id) {
        return new User(id,"redisOnly" + executeCout.incrementAndGet(),"password");
    }

    @Cacheable(cacheNames="user", key="#username")
    @Override
    public User selectByUserName(String username) {
        return null;
    }


    @Override
    public List<User> selectAll() {
        return null;
    }

    @CachePut(cacheNames="user", key="#user.id")
    @Override
    public User insert(User user) {
        // 进行一些插入操作
        return user;
    }

    @CachePut(cacheNames="user", key="#user.id")
    @Override
    public User update(User user) {
        // 进行一些更新操作
        user.setUsername("redisOnly" + executeCout.incrementAndGet());
        // 必须把更新后的用户数据返回，这样才能把它缓存到redis中
        return user;
    }

    @CacheEvict(cacheNames="user", key="#id")
    @Override
    public boolean delete(String id) {
        // 进行一些删除操作
        return true;
    }
}
