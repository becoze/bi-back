package com.becoze.biback.model.dto.chart;


import com.becoze.biback.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;


/**
 * 查询请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    /**
     * Name
     */
    private String name;

    /**
     * id
     */
    private Long id;

    /**
     * Purpose
     */
    private String goal;

    /**
     * Chart Type e.g. pie-chart, line-chart
     */
    private String chartType;

    /**
     * Creator user id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}