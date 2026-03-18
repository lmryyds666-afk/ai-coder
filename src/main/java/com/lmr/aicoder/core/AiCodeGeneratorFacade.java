package com.lmr.aicoder.core;

import com.lmr.aicoder.ai.AiCodeGeneratorService;
import com.lmr.aicoder.ai.model.HtmlCodeResult;
import com.lmr.aicoder.ai.model.MultiFileCodeResult;
import com.lmr.aicoder.exception.BusinessException;
import com.lmr.aicoder.exception.ErrorCode;
import com.lmr.aicoder.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.kafka.SslBundleSslEngineFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一请求路径（门户）
     * @param userMessage
     * @param codeGenTypeEnum
     * @return
     */
    public File generatorAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum){
        if(codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成格式为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCode(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCode(userMessage);
            default -> {
                String errorMessage="不支持的生成类型";
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }


    /**
     * 统一请求路径流式（门户）
     * @param userMessage
     * @param codeGenTypeEnum
     * @return
     */
    public Flux<String> generatorAndSaveCodeSteam(String userMessage, CodeGenTypeEnum codeGenTypeEnum){
        if(codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成格式为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCodeStream(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCodeStream(userMessage);
            default -> {
                String errorMessage="不支持的生成类型";
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }


    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
        //流式返回生成代码后保存
        StringBuilder codeBuilder = new StringBuilder();
        return result.doOnNext(chunk-> {
            //拼接实时收到的代码片
            codeBuilder.append(chunk);
        })
                .doOnComplete(() -> {
                    //流式返回完成后保存代码
                    try{
                        String completHtmlCode = codeBuilder.toString();
                        //代码解析器解析HTML代码
                        HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(completHtmlCode);
                        //将代码保存到文件
                        File saveDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
                        log.info("保存成功，路径为: {}", saveDir.getAbsolutePath());



                    }catch(Exception e){
                            log.info("保存失败: {}", e.getMessage());
                    }
                });

    }
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
        StringBuilder codeBuilder = new StringBuilder();
        return result.doOnNext(chunk-> {
            //拼接实时收到的代码片
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            try{
                String completMultiFieldCode = codeBuilder.toString();
                //代码解析器解析HTML代码
                MultiFileCodeResult multiFileCodeResult = CodeParser.parseMultiFileCode(completMultiFieldCode);
                //将代码保存到文件
                File saveDir = CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
                log.info("保存成功，路径为: {}", saveDir.getAbsolutePath());

            }catch(Exception e){
                log.info("保存失败: {}", e.getMessage());
            }
        });


    }




    /**
     * 生成html代码并保存
     * @param userMessage
     * @return
     */
    private File generateAndSaveHtmlCode(String userMessage) {
        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);

    }

    /**
     * 生成多文件代码并保存
     * @param userMessage
     * @return
     */
    private File generateAndSaveMultiFileCode(String userMessage) {
        MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        return CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
    }



}
