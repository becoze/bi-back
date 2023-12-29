package com.becoze.biback.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class GenChartByYuAiRequest implements Serializable {

    /**
     * Name
     */
    private String name;

    /**
     * User Goal
     */
    private String goal;

    /**
     * Chart Type
     */
    private String chartType;


    private static final long serialVersionUID = 1L;
}