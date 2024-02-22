package com.becoze.biback.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Scanner;

public class DlxSendDirect {

  private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";
  private static final String WORK_EXCHANGE_NAME = "work_direct_exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {

        // declare DLX exchange
        channel.exchangeDeclare(DEAD_EXCHANGE_NAME, "direct");

        // declare DLX Message queue "boss", and bind it to the exchange
        String queue1 = "boss_dlx_queue";
        channel.queueDeclare(queue1, true, false, false, null);
        channel.queueBind(queue1, DEAD_EXCHANGE_NAME, "boss");

        // declare DLX Message queue "employee", and bind it to the exchange
        String queue2 = "third_party_dlx_queue";
        channel.queueDeclare(queue2, true, false, false, null);
        channel.queueBind(queue2, DEAD_EXCHANGE_NAME, "third_party");


        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String userInput = scanner.nextLine();
            String[] strings = userInput.split(">");
            // invalid input, skip curren input (while loop)
            if (strings.length < 1){
                continue;
            }
            String message = strings[0];
            String routingKey = strings[1];

            channel.basicPublish(WORK_EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + message + "' with routing: " + routingKey);
        }
    }
  }
}