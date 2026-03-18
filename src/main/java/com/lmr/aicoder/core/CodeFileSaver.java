package com.lmr.aicoder.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.lmr.aicoder.ai.model.HtmlCodeResult;
import com.lmr.aicoder.ai.model.MultiFileCodeResult;
import com.lmr.aicoder.model.enums.CodeGenTypeEnum;
import org.springframework.web.context.annotation.ApplicationScope;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class CodeFileSaver {
    //文件保存根目录
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir")+"/tmp/code_output";

    //保存HTMLCodeResult
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult) {
        String baseDirPath = buildUniquerdir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(baseDirPath,"index.html",htmlCodeResult.getHtmlCode());
        return new File(baseDirPath);
    }

    //保存MultiFileCodeResult
    public static File saveMultiFileCodeResult(MultiFileCodeResult multiFileCodeResult){
        String baseDirPath = buildUniquerdir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(baseDirPath,"index.html",multiFileCodeResult.getHtmlCode());
        writeToFile(baseDirPath,"style.css",multiFileCodeResult.getCssCode());
        writeToFile(baseDirPath,"script.js",multiFileCodeResult.getJsCode());
        return new File(baseDirPath);
    }





    /**
     * 构建唯一目录路径：tmp/code_output/bizType_雪花ID
     *
     */
    private static String buildUniquerdir(String bizType){
        String uniqueDirName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + "/" + uniqueDirName;
        return dirPath;

    }






    /**
     * 写入单个文件
     */
    private static void writeToFile(String dirPath,String filename,String content)
    {
        String filePath = dirPath + "/" + filename;
        FileUtil.writeString(content,filePath, StandardCharsets.UTF_8);
    }



}
