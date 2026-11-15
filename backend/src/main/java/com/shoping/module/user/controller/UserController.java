package com.shoping.module.user.controller;

import com.shoping.common.result.Result;
import com.shoping.module.user.entity.MemberPointRecord;
import com.shoping.module.user.service.MemberService;
import com.shoping.module.user.service.UserService;
import com.shoping.module.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final MemberService memberService;

    @GetMapping("/{userId}")
    public Result<UserVO> get(@PathVariable Long userId) {
        return Result.ok(userService.get(userId));
    }

    /** 积分流水明细 */
    @GetMapping("/{userId}/points")
    public Result<List<MemberPointRecord>> pointRecords(@PathVariable Long userId) {
        return Result.ok(memberService.pointRecords(userId));
    }
}
