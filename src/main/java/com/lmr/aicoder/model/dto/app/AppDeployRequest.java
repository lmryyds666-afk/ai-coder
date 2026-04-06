package com.lmr.aicoder.model.dto.app;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 应用部署请求类
 */
@Data
public class AppDeployRequest {
    private Long appId;
    /**
     * 应用ID
     */
    private static final long serialVersionUID = 1L;
}
