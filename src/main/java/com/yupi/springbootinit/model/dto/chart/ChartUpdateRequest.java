package com.yupi.springbootinit.model.dto.chart;


import lombok.Data;

import java.io.Serializable;


/**
 * 更新请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class ChartUpdateRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * Purpose
     */
    private String goal;

    /**
     * Raw Chart Data
     */
    private String chartData;

    /**
     * Chart Type e.g. pie-chart, line-chart
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}