package org.hsweb.demo.service;

import org.hsweb.demo.po.User;

import java.util.List;

public interface RedisOnlyService {
    User selectById(String id);
    User selectByUserName(String username);

    List<User> selectAll();

    User insert(User user);

    User update(User user);

    boolean delete(String id);
}
