package com.lmr.aicoder;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@MapperScan("com.lmr.aicoder.mapper")
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})

public class AiCoderApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCoderApplication.class, args);
    }

}

