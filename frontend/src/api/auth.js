import request from './request'

// 登录（用户名/手机号 + 密码）
export const login = (data) => request.post('/auth/login', data)

// 注册（手机号 + 密码，注册成功即返回登录态）
export const register = (data) => request.post('/auth/register', data)

// 当前登录用户信息
export const getMe = () => request.get('/auth/me')

// 修改密码
export const changePassword = (data) => request.post('/auth/password', data)

// 登出
export const logout = () => request.post('/auth/logout')
