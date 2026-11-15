package com.shoping;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan(value = "com.shoping.module", markerInterface = BaseMapper.class)
@MapperScan(value = "com.shoping.module.stats.mapper")
public class ShopingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopingApplication.class, args);
    }
}
