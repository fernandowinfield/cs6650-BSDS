import io.swagger.client.api.SkiersApi;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;


public class Client {
  // Single server setting
  private static final String BASE_URL_PATH = "http://ec2-34-224-2-148.compute-1.amazonaws.com:8080/server_war";

  // Load balancer & 4 server instances setting
//  private static final String BASE_URL_PATH = "http://cs6650-loadBalancer-2105365642.us-east-1.elb.amazonaws.com:8080/server_war";


  /*
   * This is the main function that executes the experiment described in Assignment 1.
   *
   * NOTE: Before running the client, please make sure that there exists an 'out/' directory at the
   * same directory level as the 'client.jar 'file or '/src' directory. Also, please remember to delete
   * 'experimentResults.csv' from the 'out/' directory if there is such file. All of this will ensure
   * that the experiment runs smoothly. Thanks!
   *
   * The command line inputs, in order, are:
   * 1. 'maxThreads' - maximum number of threads to run (max 256).
   * 2. 'numSkiers' - number of skier to generate lift rides for (default 50000). This effectively will be the skier IDs (skierID).
   * 3. 'numLifts' - number of ski lifts (numLifts - range 5-60, default 40).
   * 4. 'skiDayNum' - default to 1.
   * 5. 'resortId' - the resort name which is the resortID.
   * 6. 'serverAddress' - IP/port address of the server.
   *
   * To make it easier, you can copy and run the command below. Just replace the `<x>` placeholders to configure the experiment run.
   * java Client maxThreads-<1> numSkiers-<2> numLifts-<3> skiDayNum-<4> resortId-<5> serverAddress-<6>
   *
   * For example:
   * java Client maxThreads-32 numSkiers-20000 numLifts-30 skiDayNum-2 resortId-Mission_Ridge serverAddress-http://ec2-34-224-2-148.compute-1.amazonaws.com:8080/java-server_war
   */
  public static void main(String[] args) throws InterruptedException {
    // Set default values in case they are not given as command line arguments
    Map<String, String> commandLineArgsMap = new HashMap<>();
    commandLineArgsMap.put("maxThreads", "256");
    commandLineArgsMap.put("numSkiers", "50000");
    commandLineArgsMap.put("numLifts", "40");
    commandLineArgsMap.put("skiDayNum", "1");
    commandLineArgsMap.put("resortId", "SilverMt");
    commandLineArgsMap.put("serverAddress", BASE_URL_PATH);

    // Parse command line arguments
    processCommandLineArgs(args, commandLineArgsMap);

    int maxThreads = Integer.parseInt(commandLineArgsMap.get("maxThreads"));
    int numSkiers = Integer.parseInt(commandLineArgsMap.get("numSkiers"));
    int numLifts = Integer.parseInt(commandLineArgsMap.get("numLifts"));
    String skiDayNum = commandLineArgsMap.get("skiDayNum");
    String resortId = commandLineArgsMap.get("resortId");
    String serverAddress = commandLineArgsMap.get("serverAddress");

    // General variables
    int phase1ThreadsNum = maxThreads/4;
    int phase3ThreadsNum = maxThreads/4;
    int totalThreadsNum = maxThreads + (phase1ThreadsNum * 2);
    int skierIdsPerThread = numSkiers/maxThreads;
    int currentSkierId = 1;

    final CountDownLatch partialPhase1Completed = new CountDownLatch((int)Math.ceil(phase1ThreadsNum/10));
    CountDownLatch partialPhase2Completed = new CountDownLatch((int)Math.ceil(maxThreads/10));
    CountDownLatch allPhasesCompleted = new CountDownLatch(totalThreadsNum);
    CountDownLatch allWritesToCsvCompleted = new CountDownLatch(1);

    final RequestCounterBarrier requestsCompleted = new RequestCounterBarrier();
    final RequestCounterBarrier requestsFailed = new RequestCounterBarrier();

    SkiersApi apiInstance = new SkiersApi();

    // Initialize blocking queue and start thread that will dequeue and write data to .csv file
    BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    DataWriterThread writerThread = new DataWriterThread(queue, allPhasesCompleted, allWritesToCsvCompleted);
    new Thread(writerThread).start();
    queue.put("StartTime,RequestType,Latency,ResponseCode\n");

    long startTime = System.currentTimeMillis();

    // Phase 1 START
    for (int i = 0; i < phase1ThreadsNum; i++) {
      int prevSkierId = currentSkierId;
      currentSkierId += (skierIdsPerThread/4 + 1);
      RequestsThread thread = new RequestsThread(
          prevSkierId,
          currentSkierId-1,
          1,
          90,
          serverAddress,
          apiInstance,
          requestsCompleted,
          requestsFailed,
          partialPhase1Completed,
          allPhasesCompleted,
          1,
          queue,
          resortId,
          skiDayNum,
          numLifts);
      new Thread(thread).start();
    }
    partialPhase1Completed.await();
    // Phase 1 END

    // Phase 2 START
    for (int i = 0; i < maxThreads; i++) {
      int prevSkierId = currentSkierId;
      currentSkierId += (skierIdsPerThread + 1);
      RequestsThread thread = new RequestsThread(
          prevSkierId,
          currentSkierId-1,
          91,
          360,
          serverAddress,
          apiInstance,
          requestsCompleted,
          requestsFailed,
          partialPhase2Completed,
          allPhasesCompleted,
          2,
          queue,
          resortId,
          skiDayNum,
          numLifts);
      new Thread(thread).start();
    }
    partialPhase2Completed.await();
    // Phase 2 END

    // Phase 3 START
    for (int i = 0; i < phase3ThreadsNum; i++) {
      int prevSkierId = currentSkierId;
      currentSkierId += (skierIdsPerThread/4 + 1);
      RequestsThread thread = new RequestsThread(
          prevSkierId,
          currentSkierId-1,
          361,
          420,
          serverAddress,
          apiInstance,
          requestsCompleted,
          requestsFailed,
          partialPhase1Completed,
          allPhasesCompleted,
          3,
          queue,
          resortId,
          skiDayNum,
          numLifts);
      new Thread(thread).start();
    }
    // Phase 3 END

    // wait for all threads to complete the requests
    allPhasesCompleted.await();
    long endTime = System.currentTimeMillis();
    // wait for the writer to finish writing all of the data from the blocking queue
    allWritesToCsvCompleted.await();

    // Experiment results calculations
    long durationInMilliseconds = endTime - startTime;
    double throughput = (requestsCompleted.getVal() + requestsFailed.getVal()) / (durationInMilliseconds / 1000);
    List<Integer> latenciesPOST = readLatenciesFromCsv("POST");
    List<Integer> latenciesGET1 = readLatenciesFromCsv("GET1");
    List<Integer> latenciesGET2 = readLatenciesFromCsv("GET2");
    Collections.sort(latenciesPOST);
    Collections.sort(latenciesGET1);
    Collections.sort(latenciesGET2);

    double latenciesPOSTMean = calculateMean(latenciesPOST);
    double latenciesGET1Mean = calculateMean(latenciesGET1);
    double latenciesGET2Mean = calculateMean(latenciesGET2);
    double latenciesPOSTMedian = calculateMedian(latenciesPOST);
    double latenciesGET1Median = calculateMedian(latenciesGET1);
    double latenciesGET2Median = calculateMedian(latenciesGET2);
    double latenciesPOSTPercentile99 = calculatePercentile(latenciesPOST, 99);
    double latenciesGET1Percentile99 = calculatePercentile(latenciesGET1, 99);
    double latenciesGET2Percentile99 = calculatePercentile(latenciesGET2, 99);
    int maxPOSTLatency = latenciesPOST.get(latenciesPOST.size()-1);
    int maxGET1Latency = latenciesGET1.get(latenciesGET1.size()-1);
    int maxGET2Latency = latenciesGET2.get(latenciesGET2.size()-1);

    // Print experiment results
    System.out.println("<---------API legends--------->");
    System.out.println("POST: /skiers/liftrides");
    System.out.println("GET1: /skiers/{resortID}/days/{dayID}/skiers/{skierID}");
    System.out.println("GET2: /skiers/{skierID}/vertical");
    System.out.println("<------------------------------>\n");
    System.out.println("<---------General info--------->");
    System.out.println("maxThreads: " + maxThreads);
    System.out.println("Successful requests: " + requestsCompleted.getVal());
    System.out.println("Failed requests: " + requestsFailed.getVal());
    System.out.println("Time taken to complete all requests (wall time): " + durationInMilliseconds + " milliseconds");
    System.out.println("Requests processed per second (throughput): " + throughput + " requests");
    System.out.println("<------------------------------>\n");
    System.out.println("<----Experiment run results---->");
    System.out.println("Mean response time for POSTs: " + latenciesPOSTMean + " milliseconds");
    System.out.println("Mean response time for GET1s: " + latenciesGET1Mean + " milliseconds");
    System.out.println("Mean response time for GET2s: " + latenciesGET2Mean + " milliseconds");
    System.out.println("-------");
    System.out.println("Median response time for POSTs: " + latenciesPOSTMedian + " milliseconds");
    System.out.println("Median response time for GET1s: " + latenciesGET1Median + " milliseconds");
    System.out.println("Median response time for GET2s: " + latenciesGET2Median + " milliseconds");
    System.out.println("-------");
    System.out.println("p99 response time for POSTs: " + latenciesPOSTPercentile99 + " milliseconds");
    System.out.println("p99 response time for GET1s: " + latenciesGET1Percentile99 + " milliseconds");
    System.out.println("p99 response time for GET2s: " + latenciesGET2Percentile99 + " milliseconds");
    System.out.println("-------");
    System.out.println("Max response time for POSTs: " + maxPOSTLatency + " milliseconds");
    System.out.println("Max response time for GET1s: " + maxGET1Latency + " milliseconds");
    System.out.println("Max response time for GET2s: " + maxGET2Latency + " milliseconds");
    System.out.println("<------------------------------>");
  }

  private static void processCommandLineArgs(String[] args, Map<String, String> commandLineArgsMap) {
    for (String arg: args) {
      int dashIndex = arg.indexOf("-");
      String parameter = arg.substring(0, dashIndex);
      String argument = arg.substring(dashIndex+1);
      commandLineArgsMap.put(parameter, argument);
    }
  }

  private static double calculateMean(List<Integer> itemsList) {
    int sum = 0;
    for (Integer item: itemsList) {
      sum += item;
    }
    return (double)sum/itemsList.size();
  }

  /*
   * Assumes the given list is sorted.
   */
  private static double calculateMedian(List<Integer> itemsList) {
    int middle = (itemsList.size()/2);
    if (itemsList.size()%2 == 0) {
      return (double)(itemsList.get(middle) + itemsList.get(middle-1)) / 2;
    }
    return itemsList.get(middle);
  }

  /*
   * Assumes the given list is sorted.
   */
  private static double calculatePercentile(List<Integer> itemsList, int percentile) {
    double index = ((double)percentile/100) * itemsList.size();
    if (index%1 != 0) {
      int percentileIndex = (int)Math.ceil(index);
      return (double)itemsList.get(percentileIndex-1);
    }
    int percentileIndex = (int)index;
    return ((double)itemsList.get(percentileIndex-1) + (double)itemsList.get(percentileIndex)) / 2;
  }

  private static List<Integer> readLatenciesFromCsv(String requestType) {
    List<Integer> latencies = new ArrayList<>();
    try {
      BufferedReader br = new BufferedReader(new FileReader("./out/experimentResults.csv"));
      String line;
      while ((line = br.readLine()) != null) {
        String[] values = line.split(",");
        if (values[1].equals(requestType)) {
          latencies.add(Integer.valueOf(values[2]));
        }
      }
      try {
        br.close();
      } catch (IOException e) {
        System.out.println("Something went wrong while trying close the buffered writer");
      }
    } catch (IOException e) {
      System.out.println("Something went wrong while reading from the .csv file");
    }

    return latencies;
  }
}