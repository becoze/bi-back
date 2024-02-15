package com.becoze.biback.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class RecvDirect {

  private static final String EXCHANGE_NAME = "direct_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.exchangeDeclare(EXCHANGE_NAME, "direct");

    // Message queue 1
    String queue1 = "direct_e1";
    channel.queueDeclare(queue1, true, false, false, null);
    channel.queueBind(queue1, EXCHANGE_NAME, "e1");

    // Message queue 2
    String queue2 = "direct_e2";
    channel.queueDeclare(queue2, true, false, false, null);
    channel.queueBind(queue2, EXCHANGE_NAME, "e2");

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");


    // employee1 dowork
    DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [E1] Received '" +
            delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
    };

    // employee2 dowork
    DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), "UTF-8");
      System.out.println(" [E2] Received '" +
              delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
    };
    channel.basicConsume(queue1, true, deliverCallback1, consumerTag -> { });
    channel.basicConsume(queue2, true, deliverCallback2, consumerTag -> { });
  }
}