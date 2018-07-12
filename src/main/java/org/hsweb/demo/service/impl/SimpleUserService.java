package org.hsweb.demo.service.impl;

import org.hsweb.demo.dao.UserDao;
import org.hsweb.demo.po.User;
import org.hsweb.demo.service.UserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

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
