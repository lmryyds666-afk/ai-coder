package com.lmr.aicoder.config;


import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;
    private String apiKey;
    /**
     * 流式推理模型（用于Vue项目，带工具调用
     */
    @Bean
    public StreamingChatModel reasoningStreamingChatModel(){
        //测试暂时使用chat模型
        final String modelName = "deepseek-chat";
        final int maxTokens = 8192;
        //生产环境使用
//        final String modelName = "deepseek-reasoner";
//        final int maxTokens = 32768;
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .baseUrl(baseUrl)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

}
