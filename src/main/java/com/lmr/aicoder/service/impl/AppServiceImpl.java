package com.lmr.aicoder.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.lmr.aicoder.ai.AiCodeGenTypeRoutingService;
import com.lmr.aicoder.core.AiCodeGeneratorFacade;
import com.lmr.aicoder.core.builder.VueProjectBuilder;
import com.lmr.aicoder.core.handle.StreamHandlerExecutor;
import com.lmr.aicoder.exception.BusinessException;
import com.lmr.aicoder.exception.ErrorCode;
import com.lmr.aicoder.exception.ThrowUtils;
import com.lmr.aicoder.model.constant.AppConstant;
import com.lmr.aicoder.model.dto.app.AppAddRequest;
import com.lmr.aicoder.model.dto.app.AppQueryRequest;
import com.lmr.aicoder.model.entity.User;
import com.lmr.aicoder.model.enums.ChatHistoryMessageTypeEnum;
import com.lmr.aicoder.model.enums.CodeGenTypeEnum;
import com.lmr.aicoder.model.vo.AppVO;
import com.lmr.aicoder.model.vo.UserVO;
import com.lmr.aicoder.service.ChatHistoryService;
import com.lmr.aicoder.service.ScreenshotService;
import com.lmr.aicoder.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.lmr.aicoder.model.entity.App;
import com.lmr.aicoder.mapper.AppMapper;
import com.lmr.aicoder.service.AppService;
import jakarta.annotation.Resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 *  服务层实现。
 *
 * @author 程序员李梦冉
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{
    @Resource
    @Lazy
    private ChatHistoryService chatHistoryService;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    private final UserService userService;
    private final AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Autowired
    private OrderedFormContentFilter formContentFilter;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
    @Resource
    private ScreenshotService screenshotService;
    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Value("${code.deploy-host:http://localhost}")
    private String deployHost;







    public AppServiceImpl(UserService userService, AiCodeGeneratorFacade aiCodeGeneratorFacade) {
        this.userService = userService;
        this.aiCodeGeneratorFacade = aiCodeGeneratorFacade;
    }

    /**
     * 创建应用
     * @param appAddRequest
     * @param loginUser
     * @return
     */

    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }


    /**
 * 查询应用信息方法getApp
 */
    @Override
    public AppVO getAppVO(App app) {
        //校验参数
        if(app == null){
          return null;
        }
        AppVO appVO = new AppVO();
        BeanUtils.copyProperties(app,appVO);
        //关联用户信息
        Long userId = app.getUserId();
        User user = userService.getById(userId);
        UserVO userVO = userService.getUserVO(user);
        appVO.setUser(userVO);
        return appVO;

    }

    /**
     * 构造查询条件
     * @param appQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    /**
     * 根据聊天生成代码
     * @param appId
     * @param userMessage
     * @param loginUser
     * @return
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String userMessage, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(appId == null || appId<= 0, ErrorCode.PARAMS_ERROR,"应用不能为空");
        ThrowUtils.throwIf(StrUtil.isBlankIfStr(userMessage), ErrorCode.PARAMS_ERROR,"提示词不能为空");
        //查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR,"应用不存在");

        //校验权限，仅本人可以和自己的应用对话
        if(!app.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"无权限访问该路径");
        }
        //获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR,"代码生成类型不存在");

        //添加用户消息到对话历史
        chatHistoryService.addChatMessage(appId, userMessage, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());

        //调用AI生成代码
        Flux<String> codeStream= aiCodeGeneratorFacade.generateAndSaveCodeStream(userMessage, codeGenTypeEnum, appId);

        //收集AI生成的内容，并在生成完成后保存对话历史到数据库
      return streamHandlerExecutor.doExecute(codeStream,chatHistoryService,appId,loginUser,codeGenTypeEnum);



    }

    /**
     * 应用部署
     * @param appId
     * @param loginUser
     * @return
     */
    @Override
    public String deployApp(Long appId, User loginUser) {

        //参数校验
        ThrowUtils.throwIf(appId == null || appId<= 0, ErrorCode.PARAMS_ERROR,"应用不能为空");
        //查询应用信息
        App app = this.getById(appId);

        //验证用户是否有权限部署该应用
        if(!app.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"无权限访问该路径");
        }
        //检查是否已有deployKey，每有进行刘六位生成
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlankIfStr(deployKey)){
            deployKey = RandomUtil.randomString(6);
        }
        //获取代码生成类型，构建源文件目录
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType+"_"+appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR+ File.separator+sourceDirName;
        //检查源文件是否存在
        if(! new File(sourceDirPath).exists() || !new File(sourceDirPath).isDirectory()){
           throw new BusinessException(ErrorCode.SYSTEM_ERROR,"应用不存在，请先生成应用");
       }
        //vue项目特殊处理，执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if(codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT){
            //vue项目构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess,ErrorCode.SYSTEM_ERROR,"构建失败");
            //检查dist目录是否存在
            File disDir = new File(sourceDirPath,"dist");
            ThrowUtils.throwIf(!disDir.exists() || !disDir.isDirectory(),ErrorCode.SYSTEM_ERROR,"Vue项目构建完成但未生成dist目录");
            //将dist目录赋值给source 作为部署源
            sourceDirPath = disDir.getAbsolutePath();
            log.info("Vue项目构建完成，dist目录: {}", disDir.getAbsolutePath());
        }
        //复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR+ File.separator+deployKey;
        try {
            FileUtil.copyContent(new File(sourceDirPath), new File(deployDirPath), true);
        } catch (IORuntimeException e) {
            throw new RuntimeException(e);
        }
        //数据库更新应用的deployKey和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult,ErrorCode.OPERATION_ERROR,"数据库应用部署失败");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", deployHost, deployKey);
        // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;

    }

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */

    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        new Thread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新应用封面字段
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        }).start();
    }



    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 删除应用
        return super.removeById(id);
    }



}
