package com.lmr.aicoder.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.lmr.aicoder.exception.BusinessException;
import com.lmr.aicoder.exception.ErrorCode;
import com.lmr.aicoder.exception.ThrowUtils;
import com.lmr.aicoder.model.dto.user.UserQueryRequest;
import com.lmr.aicoder.model.enums.UserRoleEnum;
import com.lmr.aicoder.model.vo.LoginUserVO;
import com.lmr.aicoder.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.lmr.aicoder.model.entity.User;
import com.lmr.aicoder.mapper.UserMapper;
import com.lmr.aicoder.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.lmr.aicoder.model.constant.UserConstant.USER_LOGIN_STATE;

/**
 *  服务层实现。
 *
 * @author 程序员李梦冉
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{


    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //1.校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if(userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        //2.检查是否重复
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount",userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if(count>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }

        //3.加密
        String encryptPassword = getEncryPassword(userPassword);

        //4.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("新用户");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if(!saveResult){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }


        return user.getId();
    }

    /**
     * 返回脱敏后的用户信息
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if(user== null)
            return null;
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user,loginUserVO);
        return loginUserVO;
    }

    /**
     * 用户登录
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验
        if(StrUtil.hasBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if(userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        //2.加密
        String encryPassword = getEncryPassword(userPassword);
        //3.查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",encryPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        //如果用户不存在
        if(user == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        //4.记录用户的登录状态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        //5.获得脱敏后的用户信息
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        //判断是否登录
        if(currentUser == null|| currentUser.getId() == null){
            ThrowUtils.throwIf(true,ErrorCode.NOT_LOGIN_ERROR);
        }
        //为了保证最新数据，根据session中查到的id查询数据库
        Long userId= currentUser.getId();
        currentUser = this.getById(userId);
        return currentUser;
    }

    /**
     * 用户注销（退出登录）
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        //判断是否登录
        if(request.getSession().getAttribute(USER_LOGIN_STATE) == null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        //登录则清除登录状态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取脱敏后的用户信息
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if(user == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        return userVO;
    }

    /**
     * 获取脱敏后的用户列表
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if(CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 构造查询条件
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if(userQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id",id)
                .eq("userRole",userRole)
                .like("userAccount",userAccount)
                .like("userName",userName)
                .like("userProfile",userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 获取加密后的密码
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryPassword(String userPassword) {
        final String SALT = "lmr";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }







}
