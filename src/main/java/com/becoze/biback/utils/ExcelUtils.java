package com.becoze.biback.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrBuilder;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Excel Utils
 * using easy excel
 */
@Slf4j
public class ExcelUtils {
    /**
     * Excel to CSV
     * @param multipartFile
     * @return
     */
    public static String excelToCsv(MultipartFile multipartFile){
//        File file = null;
//        try {
//            file = ResourceUtils.getFile("classpath:test_excel.xlsx");
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
        // gather input data
        List<Map<Integer, String>> list = null;
        // receive "multipartFile"
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            // Slf4j Exception
            log.error("Excel to CSV error!", e);
        }
        if(CollUtil.isEmpty(list)){
            return "";
        }

        // Convert to csv
        StringBuilder stringBuilder = new StringBuilder();
        // read table header
        LinkedHashMap<Integer, String> headerMap = (LinkedHashMap)list.get(0);
        // remove null cells (not empty cell)
        List<String> headerList = headerMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
        stringBuilder.append(StringUtils.join(headerList, ",")).append("\n");

        // read table content
        for(int i = 1; i < list.size(); i++){
            LinkedHashMap<Integer, String> dataMap = (LinkedHashMap) list.get(i);
            List<String> dataList = dataMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(dataList, ",")).append("\n");
        }
        return stringBuilder.toString();
    }

}
