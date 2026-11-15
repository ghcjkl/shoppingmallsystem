package com.shoping.module.auth.service;

import com.shoping.module.auth.dto.ChangePasswordRequest;
import com.shoping.module.auth.dto.LoginRequest;
import com.shoping.module.auth.dto.RefreshRequest;
import com.shoping.module.auth.dto.RegisterRequest;
import com.shoping.module.auth.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginRequest request);

    LoginVO register(RegisterRequest request);

    LoginVO refresh(RefreshRequest request);

    LoginVO me(Long userId);

    void changePassword(Long userId, ChangePasswordRequest request);
}
