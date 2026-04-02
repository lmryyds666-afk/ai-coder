package com.lmr.aicoder.ai;

import com.lmr.aicoder.ai.model.HtmlCodeResult;
import com.lmr.aicoder.ai.model.MultiFileCodeResult;
import com.lmr.aicoder.core.AiCodeGeneratorFacade;
import com.lmr.aicoder.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;


@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Test
    void generateHtmlCode() {
        HtmlCodeResult htmlCode = aiCodeGeneratorService.generateHtmlCode("做一个昆明旅游规划页面，代码不超过30行");
        Assertions.assertNotNull(htmlCode);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult multiFileCode = aiCodeGeneratorService.generateMultiFileCode("做一个我的世界游戏页面，代码不超过50行");
        Assertions.assertNotNull(multiFileCode);
    }

    @Test
    void generateVueProjectCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                "简单的个人博客网页，网页主题“奥比岛的快乐生活”，总代码量不超过 100 行",
                CodeGenTypeEnum.VUE_PROJECT, 1L);
        // 阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

}