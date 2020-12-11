import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;

public class Consumer {
  private static final String QUEUE_NAME = "postingQueue";
  private static final int CONSUMER_THREADS_NUM = 100;

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUsername("root");
    factory.setPassword("chipsNguac");
    factory.setVirtualHost("/");
    factory.setHost("ec2-18-234-121-229.compute-1.amazonaws.com");
    factory.setPort(5672);
    final Connection connection = factory.newConnection();

    // Make sure to close the RabbitMQ connection before program shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        connection.close();
      } catch (IOException e) {
        e.getStackTrace();
      }
    }));

    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        try {
          LiftRideDao liftRideDao = new LiftRideDao();
          final Channel channel = connection.createChannel();
          channel.queueDeclare(QUEUE_NAME, true, false, false, null);
//          System.out.println(" [*] Thread waiting for messages. To exit press CTRL+C");
          channel.basicQos(1);


          DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
//            System.out.println( " [x] Callback thread ID = " + Thread.currentThread().getId() + " Received '" + message + "'");

            String[] postData = message.split("-");
            String resortID = postData[0];
            String dayID = postData[1];
            String skierID = postData[2];
            String time = postData[3];
            String liftID = postData[4];
            int vertical = Integer.valueOf(liftID) * 10;

            LiftRide liftRide = new LiftRide(resortID, dayID, skierID, time, liftID, vertical);
            liftRideDao.createLiftRide(liftRide);
            liftRideDao.saveVerticalForRide(liftRide);
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
//            System.out.println(" [x] Done saving lift ride");
          };

          channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
        } catch (IOException e) {
          e.getStackTrace();
        }
      }
    };
    // start threads and block to receive messages
    for (int i = 0; i < CONSUMER_THREADS_NUM; i++) {
      new Thread(runnable).start();
    }
  }
}
