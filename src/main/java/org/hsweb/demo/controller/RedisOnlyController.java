package org.hsweb.demo.controller;

import org.hsweb.demo.po.User;
import org.hsweb.demo.service.RedisOnlyService;
import org.hsweb.demo.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


/**
 * 不查数据库，只走缓存
 */
@RequestMapping("/redisOnly")
@RestController()
public class RedisOnlyController {
    @Resource
    UserService redisOnlyService;

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public List<User> getAll() {
        return redisOnlyService.selectAll();
    }

    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
    public User getById(@PathVariable("id") String id) {
        return redisOnlyService.selectById(id);
    }

    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public User add(@RequestBody User user) {
        redisOnlyService.insert(user);
        return user;
    }

    @RequestMapping(value = "/user/{id}", method = RequestMethod.PUT)
    public User update(@PathVariable String id, @RequestBody User user) {
        user.setId(id);
        redisOnlyService.update(user);
        return user;
    }

    @RequestMapping(value = "/user/{id}", method = RequestMethod.DELETE)
    public boolean delete(@PathVariable String id) {
        return redisOnlyService.delete(id);
    }
}
