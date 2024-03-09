package com.becoze.biback.bizmq;

import com.becoze.biback.constant.BiMqConstant;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BiConsumer {

    // queues = {"..."} the name of queue listening to; ackMode = "..." the ack mode: "MANUAL" or "AUTO"
    // same concept as `channelName.basicConsume(...)`, @RabbitListener header automatic do it for us.

    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage message = {}", message);
        try {
            channel.basicAck(deliveryTag, false);
        }catch (Exception e){

        }
    }
}
