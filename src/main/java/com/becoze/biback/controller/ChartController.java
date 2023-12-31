package com.becoze.biback.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.becoze.biback.annotation.AuthCheck;
import com.becoze.biback.common.BaseResponse;
import com.becoze.biback.common.DeleteRequest;
import com.becoze.biback.common.ErrorCode;
import com.becoze.biback.common.ResultUtils;
import com.becoze.biback.constant.UserConstant;
import com.becoze.biback.exception.ThrowUtils;
import com.becoze.biback.manager.YuAiManager;
import com.becoze.biback.model.dto.chart.*;
import com.becoze.biback.model.entity.Chart;
import com.becoze.biback.model.entity.User;
import com.becoze.biback.model.vo.YuAiResponse;
import com.becoze.biback.service.ChartService;
import com.becoze.biback.service.UserService;
import com.becoze.biback.utils.ExcelUtils;
import com.becoze.biback.utils.SqlUtils;
import com.google.gson.Gson;
import com.becoze.biback.constant.CommonConstant;
import com.becoze.biback.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private YuAiManager yuAiManager;

    private final static Gson GSON = new Gson();

    // region 增删改查

    /**
     * Creat
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * Delete
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get/")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * AI Analysis upload
     *
     * @param multipartFile
     * @param genChartByYuAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<YuAiResponse> genChartByYuAi(@RequestPart("file") MultipartFile multipartFile,
                                                     GenChartByYuAiRequest genChartByYuAiRequest, HttpServletRequest request) {
        /*
         * gather user input
         */
        String name = genChartByYuAiRequest.getName();
        String goal = genChartByYuAiRequest.getGoal();
        String chartType = genChartByYuAiRequest.getChartType();

        /*
         * user input authentication, check input (goal, name) is valid
         */
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "goal is Null");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "Long name");

        /*
         * file authentication
         */
        // Check file size
        long size = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024 * 1L;   // define the size of 1 MB threshold
        ThrowUtils.throwIf( size > ONE_MB, ErrorCode.PARAMS_ERROR, "Exceed file size limit 1M");

        // Check file suffix
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);       // HuTool FileUtil.getSuffix() method
        final List<String> validFileSuffix = Arrays.asList("png", "jpg", "svg", "webp", "jpeg");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "Invalid file suffix");



        /*
         * AI model ID
         */
        // 鱼聪明模型ID 我的BI：1709156902984093697  歌曲推荐：1651468516836098050
        long biModelId = 1709156902984093697L;

        /*
         * Gather and form User Input - goal, chart type, chart name
         */
        StringBuilder userInput = new StringBuilder();
        userInput.append("Analysis goal: ").append(goal).append(". \n");

        if(StringUtils.isNotBlank(chartType)){      // Use user chart type preference if provided, or let AI decide
            userInput.append("Generate a ").append(chartType).append(" accordingly. \n");
        }else{
            userInput.append("Generate a most suitable chart").append(". \n");
        }
        userInput.append("Raw data: ").append("\n");
        String rawData = ExcelUtils.excelToCsv(multipartFile); // Excel content (raw data)
        userInput.append(rawData).append("\n");

        /*
         * Using AI to gather response
         */
        String aiResponse = yuAiManager.doChat(biModelId, userInput.toString());

        /*
         * Check AI Response is valid, splits AIGC content for data saving
         */
        String[] splits = aiResponse.split("【【【【【");
        if(splits.length < 3){      // AI generate wrong format
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI Response Error");
        }

        String genChart = splits[1].trim();
        String genResult = splits[2].trim();

        /*
         * Save data into database
         */
        User loginUser = userService.getLoginUser(request); // get login user
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(rawData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "Chart saving Error");

        /*
         * form response for front end
         */
        YuAiResponse yuAiResponse = new YuAiResponse();
        yuAiResponse.setGenChart(genChart);
        yuAiResponse.setGenResult(genResult);
        yuAiResponse.setChartId(chart.getId());


        return ResultUtils.success(yuAiResponse);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * User Edit
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        // 查询参数
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String name = chartQueryRequest.getName();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
