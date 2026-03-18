package com.lmr.aicoder.ai.model;

import jdk.jfr.Description;
import lombok.Data;

@Data
@Description("生成html代码文件结果")
public class HtmlCodeResult {

    @Description("html代码")
    private String htmlCode;

    @Description("生成代码的描述")
    private String description;
}
