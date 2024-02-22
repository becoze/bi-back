package com.becoze.biback.mq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

public class SendSingleTtl {

    private final static String QUEUE_NAME = "ttl_queue";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        // Establish connection with server or localhost
        // Create channel. Channel is the client enable us to operate the Message Queue
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            String message = "Hello World!";

//            byte[] messageBodyBytes = "Hello, world!".getBytes();
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .expiration("10000")
                    .build();
            channel.basicPublish("", QUEUE_NAME, properties, message.getBytes(StandardCharsets.UTF_8) );

//            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + message + "'");
        }
    }
}