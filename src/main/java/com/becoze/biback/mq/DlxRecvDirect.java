package com.becoze.biback.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashMap;
import java.util.Map;

public class DlxRecvDirect {

  private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";
  private static final String WORK_EXCHANGE_NAME = "work_direct_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    // declare DLX exchange
    channel.exchangeDeclare(WORK_EXCHANGE_NAME, "direct");

    // declare argument for binding routing (backend <--DEAD_EXCHANGE_NAME--> boss)
    Map<String, Object> argsBack = new HashMap<String, Object>();
    argsBack.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);  // bind backend-queue with DEAD-exchange
    argsBack.put("x-dead-letter-routing-key", "boss");           // bind DEAD-exchange with boss DEAD-queue

    // declare DLX Message queue "backend_queue",
    String queue1 = "backend_queue";
    channel.queueDeclare(queue1, true, false, false, argsBack);
    channel.queueBind(queue1, WORK_EXCHANGE_NAME, "backend");



    // declare argument for binding routing (frontend <--DEAD_EXCHANGE_NAME--> third_party)
    Map<String, Object> argsFront = new HashMap<>();
    argsFront.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME); // bind frontend-queue with DEAD-exchange
    argsFront.put("x-dead-letter-routing-key", "third_party");   // bind DEAD-exchange with third_party DEAD-queue

    // declare DLX Message queue "frontend_queue"
    String queue2 = "frontend_queue";
    channel.queueDeclare(queue2, true, false, false, argsFront);
    channel.queueBind(queue2, WORK_EXCHANGE_NAME, "frontend");

    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");


    // employee1 dowork
    DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        // nack the message
        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
        System.out.println(" [Back] Received '" +
            delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
    };

    // employee2 dowork
    DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), "UTF-8");
      // nack the message
      channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
      System.out.println(" [Front] Received '" +
              delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
    };
    channel.basicConsume(queue1, false, deliverCallback1, consumerTag -> { });
    channel.basicConsume(queue2, false, deliverCallback2, consumerTag -> { });
  }
}