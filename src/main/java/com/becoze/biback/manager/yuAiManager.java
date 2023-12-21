package com.becoze.biback.manager;

import com.becoze.biback.common.ErrorCode;
import com.becoze.biback.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class yuAiManager {
    @Resource
    private YuCongMingClient yuCongMingClient;

    /**
     * AI response
     * @param message
     * @return
     */
    public String doChat(String message){
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(000000000000000L);   // 鱼聪明模型ID
        devChatRequest.setMessage(message);

        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
        if(response == null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI response error");
        }
        return response.getData().getContent();
    }

}
