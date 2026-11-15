package com.shoping.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.module.user.entity.User;
import com.shoping.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时给演示顾客补登录凭证（仅当密码为空时写入，不会覆盖已有密码）
 * 普通用户端用手机号 + 密码登录，默认 13800000000 / user / 123456
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoAccountInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${demo.seed-enabled:true}")
    private boolean enabled;

    @Value("${demo.phone:13800000000}")
    private String demoPhone;

    @Value("${demo.username:user}")
    private String demoUsername;

    @Value("${demo.password:123456}")
    private String demoPassword;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, demoPhone)
                .last("limit 1"));
        if (user == null) {
            // 种子脚本里的 id=1 常被管理员占用，这里补建一个演示顾客，保证用户端可登录
            user = new User();
            user.setPhone(demoPhone);
            user.setUsername(availableUsername());
            user.setPassword(passwordEncoder.encode(demoPassword));
            user.setNickname("会员用户");
            user.setRole(0);
            user.setStatus(1);
            userMapper.insert(user);
            log.info("已创建演示顾客账号：{} / {}（手机号 {}）", user.getUsername(), demoPassword, demoPhone);
            return;
        }
        boolean changed = false;
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(demoPassword));
            changed = true;
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            Long used = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, demoUsername));
            if (used == null || used == 0) {
                user.setUsername(demoUsername);
                changed = true;
            }
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
            changed = true;
        }
        if (changed) {
            userMapper.updateById(user);
            log.info("已初始化演示顾客账号：{} / {}（手机号 {}）", user.getUsername(), demoPassword, demoPhone);
        }
    }

    /** 演示用户名被占用时退化为带后缀的用户名 */
    private String availableUsername() {
        Long used = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, demoUsername));
        if (used == null || used == 0) {
            return demoUsername;
        }
        return demoUsername + demoPhone;
    }
}
