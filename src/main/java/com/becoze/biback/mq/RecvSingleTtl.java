package com.becoze.biback.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RecvSingleTtl {

    private final static String QUEUE_NAME = "ttl_queue";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // declare ttl time
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-message-ttl", 15000); // 15 second
        channel.queueDeclare(QUEUE_NAME, false, false, false, args);

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        // Define what to do with the message
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");
        };
        // Confirm message been consumed
        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    }
}