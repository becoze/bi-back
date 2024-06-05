package com.becoze.biback.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.becoze.biback.annotation.AuthCheck;
import com.becoze.biback.bizmq.BiMessageProducer;
import com.becoze.biback.common.BaseResponse;
import com.becoze.biback.common.DeleteRequest;
import com.becoze.biback.common.ErrorCode;
import com.becoze.biback.common.ResultUtils;
import com.becoze.biback.constant.UserConstant;
import com.becoze.biback.exception.ThrowUtils;
import com.becoze.biback.manager.RedisLimiterManager;
import com.becoze.biback.manager.YuAiManager;
import com.becoze.biback.model.dto.chart.*;
import com.becoze.biback.model.entity.Chart;
import com.becoze.biback.model.entity.User;
import com.becoze.biback.model.vo.YuAiResponse;
import com.becoze.biback.service.ChartService;
import com.becoze.biback.service.UserService;
import com.becoze.biback.utils.ExcelUtils;
import com.becoze.biback.utils.SqlUtils;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Chart management APIs
 *
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

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    // region CRUD

    /**
     * Creat a chart
     *
     * @param chartAddRequest ChartAddRequest
     * @param request HttpServletRequest
     * @return BaseResponse - success message, with a new chart id
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
     * Delete a chart (admin only)
     *
     * @param deleteRequest DeleteRequest
     * @param request HttpServletRequest
     * @return BaseResponse - success message, with an id of deleted chart
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // Determined is the target chart exist
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // Check is admin, delete only open for admin role
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * Update chart data (admin only)
     *
     * @param chartUpdateRequest ChartUpdateRequest
     * @return BaseResponse - success message, with an id of updated chart
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
        // Determine if it exists
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * Read (get) chart by id
     *
     * @param id long, chart id
     * @return BaseResponse - success message, with a chart id
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
     * @param multipartFile MultipartFile
     * @param genChartByYuAiRequest GenChartByYuAiRequest
     * @param request HttpServletRequest
     * @return BaseResponse - success message with, YuAiResponse - chart id, user input, chart output (content)
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
        final List<String> validFileSuffix = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "Invalid file suffix");

        // Get login user
        User loginUser = userService.getLoginUser(request);

        // Rate Limiter, limit every user use current genChartByYuAi_ method
        redisLimiterManager.doRateLimit("genChartByYuAi_" + loginUser.getId());

        /*
         * AI model ID
         */
        long biModelId = CommonConstant.BI_MODEL_ID;

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
     * AI Analysis upload (Async)
     *
     * @param multipartFile MultipartFile
     * @param genChartByYuAiRequest GenChartByYuAiRequest
     * @param request HttpServletRequest
     * @return BaseResponse - success message with YuAiResponse - chart id, user input, chart output (content)
     */
    @PostMapping("/gen/async")
    public BaseResponse<YuAiResponse> genChartByYuAiAsync(@RequestPart("file") MultipartFile multipartFile,
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
        final List<String> validFileSuffix = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "Invalid file suffix");

        // Get login user
        User loginUser = userService.getLoginUser(request);

        // Rate Limiter, limit every user use current genChartByYuAi_ method
        redisLimiterManager.doRateLimit("genChartByYuAi_" + loginUser.getId());

        /*
         * AI model ID
         */
        // 鱼聪明模型ID 我的BI：1709156902984093697  歌曲推荐：1651468516836098050
        long biModelId = CommonConstant.BI_MODEL_ID;

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
         * Save data into database
         */
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(rawData);
        chart.setChartType(chartType);
        chart.setStatus("wait");        // Set chart status to wait
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "Chart saving Error");


        /*
         * Submitting "call AI requests" as a task into the queue
         */
        CompletableFuture.runAsync(() -> {
            // Change chart status to "running" when task is progressing
            Chart updateChartRunning = new Chart();
            updateChartRunning.setId(chart.getId());
            updateChartRunning.setStatus("running");
            boolean updateRunning = chartService.updateById(updateChartRunning);
            if(!updateRunning){
                handleChartStatusError(chart.getId(), "Fail to change chart status to \"running\".");
                return;
            }

            /*
             * Using AI to gather response
             */
            String aiResponse = yuAiManager.doChat(biModelId, userInput.toString());

            /*
             * Check AI Response is valid, splits AIGC content for data saving
             */
            String[] splits = aiResponse.split("【【【【【");
            if(splits.length < 3){      // Check if the AI generate the wrong format
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI Response Error");
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();

            // Change chart status to "success" when task is completed
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus("succeed");
            boolean updateResult = chartService.updateById(updateChartResult);
            if(!updateResult){
                handleChartStatusError(chart.getId(), "Fail to change chart status to \"success\".");
            }
        }, threadPoolExecutor);


        /*
         * Generate response for front end
         */
        YuAiResponse yuAiResponse = new YuAiResponse();
        yuAiResponse.setChartId(chart.getId());


        return ResultUtils.success(yuAiResponse);
    }

    /**
     * AI Analysis upload (Async + Message Queue)
     *
     * @param multipartFile MultipartFile
     * @param genChartByYuAiRequest GenChartByYuAiRequest
     * @param request HttpServletRequest
     * @return BaseResponse - success message with YuAiResponse - chart id, user input, chart output (content)
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<YuAiResponse> genChartByYuAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
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
        final List<String> validFileSuffix = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "Invalid file suffix");

        // Get login user
        User loginUser = userService.getLoginUser(request);

        // Rate Limiter, limit every user use current genChartByYuAi_ method
        redisLimiterManager.doRateLimit("genChartByYuAi_" + loginUser.getId());

        /*
         * AI model ID
         */
        long biModelId = CommonConstant.BI_MODEL_ID;

        /*
        * Building user analysis requirement to send to AI
         */
        StringBuilder userInput = new StringBuilder();

        userInput.append("Analysis goal: ").append(goal).append(". \n");

        if(StringUtils.isNotBlank(chartType)){      // Use user chart type preference if provided, or let AI decide
            userInput.append("Generate a ").append(chartType).append(" accordingly. \n");
        } else {
            userInput.append("Generate a most suitable chart").append(". \n");
        }

        // turn user uploaded excel file to scv file
        userInput.append("Raw data: ").append("\n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        /*
         * Save data into database
         */
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");        // Set chart status to wait
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "Chart saving Error");

        /*
         * Send ID to message queue
         */
        Long newChartId = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(newChartId));


        /*
         * Generate response for front end
         */
        YuAiResponse yuAiResponse = new YuAiResponse();
        yuAiResponse.setChartId(newChartId);


        return ResultUtils.success(yuAiResponse);
    }


    private void handleChartStatusError(long chartId, String execMessage){
        // Change chart status to fail
        Chart updateChartFail = new Chart();
        updateChartFail.setId(chartId);
        updateChartFail.setStatus("failed");
        updateChartFail.setExecMessage(execMessage);
        boolean updateFail = chartService.updateById(updateChartFail);
        if(!updateFail){
            log.error("Fail to change chart status to \"fail\". Chart id: " + chartId + " , message: " + execMessage);
        }
    }

    /**
     * Paginated List Retrieval (Encapsulated Class)
     *
     * @param chartQueryRequest ChartQueryRequest
     * @param request HttpServletRequest
     * @return
     */
    @PostMapping("/list/page/")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // Restricting Crawlers
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * Paginated Retrieval of Resource List Created by the Current User
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
        // Restricting Crawlers
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * User update / edit chart data
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
        // Determine if it exists
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // Verifying User Identity
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * Gather query wrapper
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        // Query Parameters
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String name = chartQueryRequest.getName();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        // Concatenating Queries
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
