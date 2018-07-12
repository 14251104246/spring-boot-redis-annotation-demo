package org.hsweb.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Configuration
@EnableAutoConfiguration
@ComponentScan("org.hsweb.demo")
@MapperScan("org.hsweb.demo.dao")
@EnableCaching//开启注解驱动的缓存管理
@Controller
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @RequestMapping({"/", "/index.html"})
    public ModelAndView index() {
        return new ModelAndView("index");
    }

}
