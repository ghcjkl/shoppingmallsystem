package com.shoping.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.module.user.entity.Member;
import com.shoping.module.user.entity.User;
import com.shoping.module.user.mapper.MemberMapper;
import com.shoping.module.user.mapper.UserMapper;
import com.shoping.module.user.service.UserService;
import com.shoping.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final MemberMapper memberMapper;

    @Override
    public UserVO get(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        Member member = memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getUserId, userId));
        return UserVO.from(user, member);
    }
}
