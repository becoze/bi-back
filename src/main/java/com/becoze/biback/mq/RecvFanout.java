package com.becoze.biback.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class RecvFanout {
  private static final String EXCHANGE_NAME = "fanout-exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();

    Channel channel1 = connection.createChannel();
    Channel channel2 = connection.createChannel();
    // Declare exchange
    channel1.exchangeDeclare(EXCHANGE_NAME, "fanout");
    channel2.exchangeDeclare(EXCHANGE_NAME, "fanout");

    // Create task queues
    String employee1 = "employee_1";
    // Implement queue
    channel1.queueDeclare(employee1, true, false, false, null);
    // Bind the queue with the exchange
    channel1.queueBind(employee1, EXCHANGE_NAME, "");


    String employee2 = "employee_2";
    channel2.queueDeclare(employee2, true, false, false, null);
    channel2.queueBind(employee2, EXCHANGE_NAME, "");
    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    // do work
    DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [E1] Received '" + message + "'");
    };

    DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [E2] Received '" + message + "'");
    };

    // listening
    channel1.basicConsume(employee1, true, deliverCallback1, consumerTag -> { });
    channel2.basicConsume(employee2, true, deliverCallback2, consumerTag -> { });
  }
}