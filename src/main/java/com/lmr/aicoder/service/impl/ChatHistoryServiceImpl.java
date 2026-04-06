package com.lmr.aicoder.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.lmr.aicoder.exception.ErrorCode;
import com.lmr.aicoder.exception.ThrowUtils;
import com.lmr.aicoder.model.constant.UserConstant;
import com.lmr.aicoder.model.dto.chathistory.ChatHistoryQueryRequest;
import com.lmr.aicoder.model.entity.App;
import com.lmr.aicoder.model.entity.User;
import com.lmr.aicoder.model.enums.ChatHistoryMessageTypeEnum;
import com.lmr.aicoder.service.AppService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.lmr.aicoder.model.entity.ChatHistory;
import com.lmr.aicoder.mapper.ChatHistoryMapper;
import com.lmr.aicoder.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.CharacterTypeHandler;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 *  服务层实现。
 *
 * @author 程序员李梦冉
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{
    @Resource
    private AppService appService;
    /**
     * 新增对话历史
     * @param appId
     * @param message
     * @param messageType
     * @param userId
     * @return
     */
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        //参数校验
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR,"应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlankIfStr( message), ErrorCode.PARAMS_ERROR,"消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlankIfStr( messageType), ErrorCode.PARAMS_ERROR,"消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId < 0, ErrorCode.PARAMS_ERROR,"用户ID不能为空");
        //验证消息类型是否有效
        ChatHistoryMessageTypeEnum enumByValue = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(enumByValue == null, ErrorCode.PARAMS_ERROR,"无效的消息类型"+messageType);
        //创建对话历史记录到数据库
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();

        return this.save(chatHistory);
    }


    /**
     * 删除对话历史
     * @param appId
     * @return
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR,"应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }


    /**
     * 获取查询包装类的构造方法
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

    /**
     * 编写游标查询服务方法
     * @param appId
     * @param pageSize
     * @param lastCreateTime
     * @param loginUser
     * @return
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize, LocalDateTime lastCreateTime, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(appId == null || appId < 0, ErrorCode.PARAMS_ERROR,"应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0||pageSize > 50, ErrorCode.PARAMS_ERROR,"分页大小不能小于1且大于50");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //权限验证功能，只有应用创建者和管理员才能查看应用下的对话历史
        App app = appService.getById(appId);
        if (!loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE) && !app.getUserId().equals(loginUser.getId())) {
            ThrowUtils.throwIf(true, ErrorCode.NO_AUTH_ERROR,"无权限查看该应用的对话历史");
        }
        //构建查询条件
        ChatHistoryQueryRequest chatHistoryQueryRequest = ChatHistoryQueryRequest.builder()
                .appId(appId)
                .lastCreateTime(lastCreateTime)
                .build();
        QueryWrapper queryWrapper = this.getQueryWrapper(chatHistoryQueryRequest);
        //查询数据

        return this.page(Page.of(1, pageSize),queryWrapper);
    }


    /**
     * 从数据库中加载历史到对话中
     * @param appId
     * @param chatMemory
     * @param maxCount
     * @return
     */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }


}
