import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class DataWriterThread implements Runnable {
  private BlockingQueue<String> queue;
  private CountDownLatch allPhasesCompleted;
  private CountDownLatch allWritesToCsvCompleted;
  private FileWriter fileWriter;
  private BufferedWriter bufferedWriter;

  public DataWriterThread(BlockingQueue<String> blockingQueue,
                          CountDownLatch allPhasesCompleted,
                          CountDownLatch allWritesToCsvCompleted) {
    this.queue = blockingQueue;
    this.allPhasesCompleted = allPhasesCompleted;
    this.allWritesToCsvCompleted = allWritesToCsvCompleted;
    try {
      this.fileWriter = new FileWriter("./out/experimentResults.csv",true);
      this.bufferedWriter = new BufferedWriter(this.fileWriter);
    } catch (Exception e) {
      System.out.println("Something went wrong initializing the writers (.csv)");
    }
  }

  @Override
  public void run() {
    while (allPhasesCompleted.getCount() > 0 || !queue.isEmpty()) {
      try {
        String dataToWrite = queue.take();
        try {
          this.bufferedWriter.write(dataToWrite);
        } catch (IOException e) {
          System.out.println("Something went wrong while trying to write data to the .csv file");
        }
      } catch (InterruptedException e) {
        System.out.println("Something went wrong while taking from the blocking queue");
      }
    }

    try {
      this.bufferedWriter.close();
    } catch (IOException e) {
      System.out.println("Something went wrong while trying close the buffered writer");
    }

    allWritesToCsvCompleted.countDown();
  }
}
