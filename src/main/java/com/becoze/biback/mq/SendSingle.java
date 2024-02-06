package com.becoze.biback.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

public class SendSingle {

    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv) throws Exception {
        // Create connection factory
        ConnectionFactory factory = new ConnectionFactory();
        // Config connection factory
        factory.setHost("localhost");
        // More config: username, password, and port if you have server
//        factory.setUsername();
//        factory.setPassword();
//        factory.setPort();

        // Establish connection with server or localhost
        // Create channel. Channel is the client enable us to operate the Message Queue
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // Create Message Queue (MUST exact same as the receiver)
            //  the queue creation will not realize if the MQ already exist,
            //  the creation code in both sender and receiver to make sure the queue exists.
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            // Send message
            String message = "Hello World!";
            // Send the message to MQ
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + message + "'");
        }
    }
}