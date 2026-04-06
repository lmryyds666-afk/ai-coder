package com.lmr.aicoder.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lmr.aicoder.ai.tools.FileWriteTool;
import com.lmr.aicoder.exception.BusinessException;
import com.lmr.aicoder.exception.ErrorCode;
import com.lmr.aicoder.model.enums.CodeGenTypeEnum;
import com.lmr.aicoder.service.ChatHistoryService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.apache.bcel.classfile.Code;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.SslBundleSslEngineFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;

//创建工厂来初始化AI服务
@Slf4j
@Configuration
public class AiCodeGeneratorServiceFactory {
    @Resource
    private ChatModel chatModel;

    @Autowired
    private StreamingChatModel openAiStreamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Lazy
    @Resource
    private ChatHistoryService chatHistoryService;
    /**
     * AI服务实例缓存
     * 缓存策略：
     * 最大缓存1000个实例
     * 写入后30分钟过期
     * 访问后10分值过期
     */
    private final Cache<String,AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI服务实例被移除，appId:{},原因:{}",key,cause.toString());
            })
            .build();
    @Autowired
    private StreamingChatModel reasoningStreamingChatModel;
//    @Autowired
//    private StreamingChatModel streamingChatModel;


    /**
     *根据appId获取AI服务(带缓存)
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId) {
        return getAiCodeGeneratorService(appId,CodeGenTypeEnum.HTML);
    }

    /**
     * 根据AppID和代码生成类型创建AI服务实例（带缓存）
     * @param appId
     * @param codeGenType
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType){
        String cacheKey = buildCacheKey(appId, codeGenType);
        //缓存实例取数据操作
        AiCodeGeneratorService service = serviceCache.getIfPresent(cacheKey);
        if(service == null){
            service = createAiCodeGeneratorService(appId,codeGenType);
            serviceCache.put(cacheKey,service);
        }
        return service;

    }

    /**
     * 构建缓存键
     * @param appId
     * @param codeGenType
     * @return
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }


    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }


    /**
     * 创建新的AI服务实例
     */
    public AiCodeGeneratorService createAiCodeGeneratorService(Long appId) {
        log.info("创建新的AI服务实例，appId:{}",appId);
        //根据APPID 构建独立的对话记忆
        MessageWindowChatMemory chatMemory=MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        //从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId,chatMemory,20);
                return AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .build();
    }


    /**
     * 创建新的AI服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenType) {
        //根据APPID 构建独立的对话记忆
        MessageWindowChatMemory chatMemory=MessageWindowChatMemory
                .builder()
                .id(appId)// 对话ID，用于区分不同用户/会话的记忆
                .chatMemoryStore(redisChatMemoryStore)// 用Redis存储对话历史
                .maxMessages(50)// 窗口大小：只保留最近20条消息
                .build();

        //从数据库加载数据到历史对话中
        chatHistoryService.loadChatHistoryToMemory(appId,chatMemory,20);
        //根据代码类型生成不同的模型配置
        return switch (codeGenType) {
            //vue项目使用推理模型
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(new FileWriteTool())
                    .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                            toolExecutionRequest, "Error:there is no tool called" + toolExecutionRequest.name()
                    ))
                    .build();
            //html模式、多文件模式使用推理模型
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(openAiStreamingChatModel)
                    .chatMemory(chatMemory)
                    .build();
            default ->
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型" + codeGenType.getValue());


        };

    }

}
