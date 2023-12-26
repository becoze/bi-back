package com.becoze.biback.model.vo;

import lombok.Data;

/**
 * Response of YuAiManager
 */

@Data
public class YuAiResponse {

    private String genChart;

    private String genResult;

    private Long chartId;

}
