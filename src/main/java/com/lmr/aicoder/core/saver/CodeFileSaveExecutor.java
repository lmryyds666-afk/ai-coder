package com.lmr.aicoder.core.saver;

import com.lmr.aicoder.ai.model.HtmlCodeResult;
import com.lmr.aicoder.ai.model.MultiFileCodeResult;
import com.lmr.aicoder.exception.BusinessException;
import com.lmr.aicoder.exception.ErrorCode;
import com.lmr.aicoder.model.enums.CodeGenTypeEnum;

import java.io.File;


/**
 * 代码文件保存执行器
 * 根据代码生成类型执行相应的保存逻辑
 *
 * @author yupi
 */
public class CodeFileSaveExecutor {

    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaver = new HtmlCodeFileSaverTemplate();

    private static final MultiFileCodeFileSaveTemplate multiFileCodeFileSaver = new MultiFileCodeFileSaveTemplate();

    /**
     * 执行代码保存
     *
     * @param codeResult  代码结果对象
     * @param codeGenType 代码生成类型
     * @return 保存的目录
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType, Long appId) {
        return switch (codeGenType) {
            case HTML -> htmlCodeFileSaver.saveCode((HtmlCodeResult) codeResult,appId);
            case MULTI_FILE -> multiFileCodeFileSaver.saveCode((MultiFileCodeResult) codeResult,appId);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType);
        };
    }
}
