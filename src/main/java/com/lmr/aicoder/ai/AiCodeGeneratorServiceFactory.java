package com.lmr.aicoder.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

//创建工厂来初始化AI服务
@Slf4j
@Configuration
public class AiCodeGeneratorServiceFactory {
    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    /**
     * AI服务实例缓存
     * 缓存策略：
     * 最大缓存1000个实例
     * 写入后30分钟过期
     * 访问后10分值过期
     */
    private final Cache<Long,AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI服务实例被移除，appId:{},原因:{}",key,cause.toString());
            })
            .build();


    /**
     *根据appId获取AI服务(带缓存)
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(Long appId) {
        //跟库appId构建独立的对话记忆
        return serviceCache.get(appId,this::createAiCodeGeneratorService);
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
                return AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(streamingChatModel)
                        .chatMemory(chatMemory)
                        .build();
    }

}
