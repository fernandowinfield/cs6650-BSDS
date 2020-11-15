import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class Consumer {
  private static final String QUEUE_NAME = "postingQueue";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();

    factory.setUsername("root");
    factory.setPassword("chipsNguac");
    factory.setVirtualHost("/");
    factory.setHost("ec2-100-25-170-47.compute-1.amazonaws.com");
    factory.setPort(5672);
    final Connection connection = factory.newConnection();



    final Channel channel = connection.createChannel();

    channel.queueDeclare(QUEUE_NAME, true, false, false, null);
    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

    channel.basicQos(1);

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), "UTF-8");
      System.out.println(" [x] Received '" + message + "'");

      String[] postData = message.split("-");
      String resortID = postData[0];
      String dayID = postData[1];
      String skierID = postData[2];
      String time = postData[3];
      String liftID = postData[4];
      int vertical = Integer.valueOf(liftID) * 10;

      LiftRide liftRide = new LiftRide(resortID, dayID, skierID, time, liftID, vertical);
      LiftRideDao liftRideDao = new LiftRideDao();
      liftRideDao.createLiftRide(liftRide);

      System.out.println(" [x] Done");
      channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    };
    channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
  }
}
