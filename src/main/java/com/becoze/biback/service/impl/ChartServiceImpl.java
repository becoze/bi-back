package com.becoze.biback.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.becoze.biback.model.entity.Chart;
import com.becoze.biback.service.ChartService;
import com.becoze.biback.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author liyua
* @description 针对表【chart(Chart)】的数据库操作Service实现
* @createDate 2023-09-22 00:30:09
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}




