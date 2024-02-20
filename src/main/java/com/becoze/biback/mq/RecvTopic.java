package com.becoze.biback.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class RecvTopic {

  private static final String EXCHANGE_NAME = "topic_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.exchangeDeclare(EXCHANGE_NAME, "topic");

      // Message queue 1
      String queue1 = "frontend_queue";
      channel.queueDeclare(queue1, true, false, false, null);
      channel.queueBind(queue1, EXCHANGE_NAME, "#.frontend.#");

      // Message queue 2
      String queue2 = "backend_queue";
      channel.queueDeclare(queue2, true, false, false, null);
      channel.queueBind(queue2, EXCHANGE_NAME, "#.backend.#");

      // Message queue 3
      String queue3 = "market_queue";
      channel.queueDeclare(queue3, true, false, false, null);
      channel.queueBind(queue3, EXCHANGE_NAME, "#.market.#");

      System.out.println(" [*] Waiting for messages. To exit press CTRL+C");


      // front end dowork
      DeliverCallback deliverCallback_Nick = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          System.out.println(" [Nick] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };

      // back end dowork
      DeliverCallback deliverCallback_Alice = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          System.out.println(" [Alice] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };
      // market dowork
      DeliverCallback deliverCallback_Tom = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), "UTF-8");
          System.out.println(" [Tom] Received '" +
                  delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
      };

      channel.basicConsume(queue1, true, deliverCallback_Nick, consumerTag -> { });
      channel.basicConsume(queue2, true, deliverCallback_Alice, consumerTag -> { });
      channel.basicConsume(queue3, true, deliverCallback_Tom, consumerTag -> { });
  }
}