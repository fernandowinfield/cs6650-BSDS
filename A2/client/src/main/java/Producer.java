import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class Producer implements Runnable{
  private BlockingQueue queue;
  private CountDownLatch allPhasesCompleted;

  public Producer(BlockingQueue<String> queue, CountDownLatch allPhasesCompleted) {
    this.queue = queue;
    this.allPhasesCompleted = allPhasesCompleted;
  }

  public void run() {
    try {
      queue.put("1,2,3,4\n");
      this.allPhasesCompleted.countDown();
      Thread.sleep(1000);
      queue.put("2,2,3,4\n");
      this.allPhasesCompleted.countDown();
      Thread.sleep(1000);
      queue.put("3,2,3,4\n");
      this.allPhasesCompleted.countDown();
      Thread.sleep(1000);
      queue.put("4,2,3,4\n");
      this.allPhasesCompleted.countDown();
    } catch (InterruptedException e) {
      System.out.println("Something went wrong with the Producer");
    }
  }
}
