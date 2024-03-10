package com.becoze.biback.bizmq;

import com.becoze.biback.common.ErrorCode;
import com.becoze.biback.constant.BiMqConstant;
import com.becoze.biback.constant.CommonConstant;
import com.becoze.biback.exception.BusinessException;
import com.becoze.biback.manager.YuAiManager;
import com.becoze.biback.model.entity.Chart;
import com.becoze.biback.service.ChartService;
import com.becoze.biback.utils.ExcelUtils;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
public class BiMessageConsumer {
    @Resource
    private ChartService chartService;

    @Resource
    private YuAiManager yuAiManager;


    // queues = {"..."} the name of queue listening to; ackMode = "..." the ack mode: "MANUAL" or "AUTO"
    // same concept as `channelName.basicConsume(...)`, @RabbitListener header automatic do it for us.

    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        if (StringUtils.isBlank(message)) {
            // Message NACK
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Message is null");
        }

        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);

        if (chart == null){
            // Message NACK
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Chart is null");
        }

        // Change chart status to "running" when task is progressing
        Chart updateChartRunning = new Chart();
        updateChartRunning.setId(chart.getId());
        updateChartRunning.setStatus("running");
        boolean b = chartService.updateById(updateChartRunning);
        if(!b){
            // Message NACK
            channel.basicNack(deliveryTag, false, false);
            handleChartStatusError(chart.getId(), "Fail to change chart status to \"running\".");
            return;
        }

        /*
         * Using AI to gather response
         */
        String aiResponse = yuAiManager.doChat(CommonConstant.BI_MODEL_ID, buildUserInput(chart));

        /*
         * Check AI Response is valid, splits AIGC content for data saving
         */
        String[] splits = aiResponse.split("【【【【【");
        if(splits.length < 3){      // Check if the AI generate the wrong format
            // Message NACK
            channel.basicNack(deliveryTag, false, false);
            handleChartStatusError(chart.getId(), "AI Response Error");
            return;
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI Response Error");
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
            // Message NACK
            channel.basicNack(deliveryTag, false, false);
            handleChartStatusError(chart.getId(), "Fail to change chart status to \"success\".");
        }
        // Message ACK
        channel.basicAck(deliveryTag, false);
    }

    /**
     * build user input using generated chart id with database
     * @param chart generated chart id
     * @return
     */
    private String buildUserInput(Chart chart) {
        /*
         * Gather and form User Input - goal, chart type, chart name
         */
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        StringBuilder userInput = new StringBuilder();
        userInput.append("Analysis goal: ").append(goal).append(". \n");

        if(StringUtils.isNotBlank(chartType)){      // Use user chart type preference if provided, or let AI decide
            userInput.append("Generate a ").append(chartType).append(" accordingly. \n");
        } else {
            userInput.append("Generate a most suitable chart").append(". \n");
        }

        userInput.append("Raw data: ").append("\n");
        userInput.append(csvData).append("\n");
        return userInput.toString();
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
}
