package com.becoze.biback.mapper;

import com.becoze.biback.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

/**
* @author becoze
* @description Database operation mapper for the table "Chart(chart)"
* @createDate 2023-09-22 00:30:09
* @Entity com.becoze.biback.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {

    List<Map<String, Object>> queryChartData(String querySql);

}




