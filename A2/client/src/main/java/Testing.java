import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;


public class Testing {
  public static void main(String[] args) {
//    testBlockingQueueWorksAndWriteToCSV();
    testReadFromCsv();
  }

  public static void testReadFromCsv() {
    List<String[]> dataRows = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader("./out/experimentResults.csv"))) {
      String line;
      while ((line = br.readLine()) != null) {
        String[] values = line.split(",");
        dataRows.add(values);
      }
    } catch (IOException e) {
      System.out.println("Something went wrong while reading the .csv file");
    }

    for (String[] row:dataRows) {
      System.out.println(row[0] + row[1] + row[2] + row[3]);
    }
  }

  public static void testWriteToCsv() {
    try {
      FileWriter fw = new FileWriter("./out/test.csv",true);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write("1,2,3,4\n");
      // Remember to close BufferedWriter
      bw.close();
    } catch (Exception e) {
      System.out.println("Something went wrong writing to .csv file");
    }
  }

  public static void testBlockingQueueWorksAndWriteToCSV() {
    BlockingQueue queue = new LinkedBlockingQueue();
    CountDownLatch allPhasesCompleted = new CountDownLatch(4);
    CountDownLatch allWritesFakeCopy = new CountDownLatch(1);

    Producer producer = new Producer(queue, allPhasesCompleted);
    DataWriterThread thread = new DataWriterThread(queue, allPhasesCompleted, allWritesFakeCopy);

    new Thread(producer).start();
    new Thread(thread).start();
  }
}
