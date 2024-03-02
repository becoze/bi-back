package com.becoze.biback.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Main {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.exchangeDeclare(EXCHANGE_NAME, "direct");

    // Message queue 1
    String queue1 = "direct_e1";
    channel.queueDeclare(queue1, true, false, false, null);
    channel.queueBind(queue1, EXCHANGE_NAME, "e1");

}
