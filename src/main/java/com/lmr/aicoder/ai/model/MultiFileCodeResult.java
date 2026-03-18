package com.lmr.aicoder.ai.model;

import jdk.jfr.Description;
import lombok.Data;

@Data
@Description("多文件代码结果")
public class MultiFileCodeResult {

    @Description("html代码")
    private String htmlCode;

    @Description("css代码")
    private String cssCode;

    @Description("js代码")
    private String jsCode;

    @Description("生成代码的描述")
    private String description;
}
