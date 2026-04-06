package com.lmr.aicoder.service;

import ch.qos.logback.classic.spi.EventArgUtil;
import com.lmr.aicoder.model.dto.user.UserQueryRequest;
import com.lmr.aicoder.model.vo.LoginUserVO;
import com.lmr.aicoder.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.lmr.aicoder.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jdk.jshell.EvalException;

import java.util.List;

/**
 *  服务层。
 *
 * @author 程序员李梦冉
 */
public interface UserService extends IService<User> {
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 返回登录后的脱敏的用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);


    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);


    /**
     * 用户注销(推出登录)
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户列表
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 将查询请求转换为queryWrapper对象
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 加密密码
     */
    String getEncryPassword(String userPassword);
}


