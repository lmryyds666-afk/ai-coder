package com.lmr.aicoder.service;

import com.lmr.aicoder.model.dto.app.AppDeployRequest;
import com.lmr.aicoder.model.dto.app.AppQueryRequest;
import com.lmr.aicoder.model.entity.User;
import com.lmr.aicoder.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.lmr.aicoder.model.entity.App;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 *  服务层。
 *
 * @author 程序员李梦冉
 */
public interface AppService extends IService<App> {
    /**
     * 用户查询应用详情
     *
     */

    public AppVO getAppVO(App app);

    /**
     * 构造查询条件
     * @param appQueryRequest
     * @return
     */
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取脱敏后的用户列表
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 根据聊天生成用户代码
     * @param appId
     * @param userMessage
     * @param loginUser
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String userMessage, User loginUser);

    /**
     * 部署应用
     * @param appId
     * @param loginUser
     * @return
     */
    String deployApp(Long appId, User loginUser);

//
}


