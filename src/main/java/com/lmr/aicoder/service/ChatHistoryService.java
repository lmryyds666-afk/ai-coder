package com.lmr.aicoder.service;

import com.lmr.aicoder.model.dto.chathistory.ChatHistoryQueryRequest;
import com.lmr.aicoder.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.lmr.aicoder.model.entity.ChatHistory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 *  服务层。
 *
 * @author 程序员李梦冉
 */
public interface ChatHistoryService extends IService<ChatHistory> {
    /**
     * 新增对话历史
     * @param appId
     * @param message
     * @param messageType
     * @param userId
     * @return
     */
    public boolean addChatMessage(Long appId,String message,String messageType,Long userId);

    /**
     * 删除对话历史
     * @param appId
     * @return
     */
    public boolean deleteByAppId(Long appId);

    /**
     * 构造查询条件构造方法
     * @param chatHistoryQueryRequest
     * @return
     */
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);


    /**
     * 游标查询方法
     */
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId,
                                                      int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser);


    /**
     * 加载对话历史到内存
     * @param appId
     * @param chatMemory
     * @param maxCount
     * @return
     */
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
