package com.becoze.biback.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.nio.charset.StandardCharsets;

public class RecvSingle {

    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv) throws Exception {
        // Create connection factory
        ConnectionFactory factory = new ConnectionFactory();
        // Config connection factory
        factory.setHost("localhost");

        // Establish connection with server or localhost
        Connection connection = factory.newConnection();
        // Create Channel
        Channel channel = connection.createChannel();

        // Create Message Queue (MUST exact same as the sender)
        //  the queue creation will not realize if the MQ already exist,
        //  the creation code in both sender and receiver to make sure the queue exists.
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        // Define what to do with the message
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");
        };
        // Confirm message been consumed
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
    }
}