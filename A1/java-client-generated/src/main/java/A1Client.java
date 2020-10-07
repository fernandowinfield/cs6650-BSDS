import io.swagger.client.api.SkiersApi;
import java.util.*;
import java.util.concurrent.CountDownLatch;


public class A1Client {
  private static final String BASE_URL_PATH = "http://ec2-34-224-2-148.compute-1.amazonaws.com:8080/java-server_war";

  /*
   * This is the main function that executes the experiment described in Assignment 1.
   * The command line inputs, in order, are:
   * 1. 'maxThreads' - maximum number of threads to run (max 256).
   * 2. 'numSkiers' - number of skier to generate lift rides for (default 50000). This effectively will be the skier IDs (skierID).
   * 3. 'numLifts' - number of ski lifts (numLifts - range 5-60, default 40).
   * 4. 'skiDayNum' - default to 1.
   * 5. 'resortId' - the resort name which is the resortID.
   * 6. 'serverAddress' - IP/port address of the server.
   *
   * To make it easier, you can copy and run the command below. Just replace the `<x>` placeholders to configure the experiment run.
   * java A1Client maxThreads-<1> numSkiers-<2> numLifts-<3> skiDayNum-<4> resortId-<5> serverAddress-<6>
   *
   * For example:
   * java A1Client maxThreads-20 numSkiers-100 numLifts-30 skiDayNum-2 resortId-Mission_Ridge serverAddress-http://ec2-34-224-2-148.compute-1.amazonaws.com:8080/java-server_war
   */
  public static void main(String[] args) throws InterruptedException {
    // Parse command line arguments
    Map<String, String> commandLineArgsMap = new HashMap<>();
    commandLineArgsMap.put("maxThreads", "256");
    commandLineArgsMap.put("numSkiers", "50000");
    commandLineArgsMap.put("numLifts", "40");
    commandLineArgsMap.put("skiDayNum", "1");
    commandLineArgsMap.put("resortId", "SilverMt");
    commandLineArgsMap.put("serverAddress", BASE_URL_PATH);

    processCommandLineArgs(args, commandLineArgsMap);

    int maxThreads = Integer.parseInt(commandLineArgsMap.get("maxThreads"));
    int numSkiers = Integer.parseInt(commandLineArgsMap.get("numSkiers"));
    int numLifts = Integer.parseInt(commandLineArgsMap.get("numLifts"));
    int skiDayNum = Integer.parseInt(commandLineArgsMap.get("skiDayNum"));
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

    final RequestCounterBarrier requestsCompleted = new RequestCounterBarrier();
    final RequestCounterBarrier requestsFailed = new RequestCounterBarrier();

    SkiersApi apiInstance = new SkiersApi();

    OpenCSVWriter writer = new OpenCSVWriter("./out/clientPart2Results.csv");

    /*
    OpenCSVWriter writer = new OpenCSVWriter("./out/clientPart2Results.csv");
    String[] dataToWrite = {"start time", "request type", "latency", "response code"};
    writer.writeDataRowToCsv(dataToWrite);
    writer.closeWriter();
    */

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
          writer);
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
          writer);
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
          writer);
      new Thread(thread).start();
    }
    // Phase 3 END

    allPhasesCompleted.await();

    long endTime = System.currentTimeMillis();
    long durationInMilliseconds = endTime - startTime;
    double throughput = (requestsCompleted.getVal() + requestsFailed.getVal()) / (durationInMilliseconds / 1000);
    List<Integer> latencies = getLatenciesFromWriter(writer);
    Collections.sort(latencies);
    double latenciesMean = calculateMean(latencies);
    double latenciesMedian = calculateMedian(latencies);
    int maxPostLatency = findMaxLatency(writer, "POST");
    int maxGetLatency = findMaxLatency(writer, "GET");
    double latenciesPercentile99 = calculatePercentile(latencies, 99);

    writer.writeAllDataRowsToCsv();
    writer.closeWriter();

    System.out.println("maxThreads: " + maxThreads);
    System.out.println("Successful requests: " + requestsCompleted.getVal());
    System.out.println("Failed requests: " + requestsFailed.getVal());
    System.out.println("Mean response time for POSTs and GETs: " + latenciesMean + " milliseconds");
    System.out.println("Median response time for POSTs and GETs: " + latenciesMedian + " milliseconds");
    System.out.println("Time taken to complete all requests (wall time): " + durationInMilliseconds + " milliseconds");
    System.out.println("Requests processed per second (throughput): " + throughput);
    System.out.println("p99 response time for POSTs and GETs: " + latenciesPercentile99 + " milliseconds");
    System.out.println("Max response time for POSTs: " + maxPostLatency + " milliseconds");
    System.out.println("Max response time for GETs: " + maxGetLatency + " milliseconds");
  }

  private static void processCommandLineArgs(String[] args, Map<String, String> commandLineArgsMap) {
    for (String arg: args) {
      int dashIndex = arg.indexOf("-");
      String parameter = arg.substring(0, dashIndex);
      String argument = arg.substring(dashIndex+1);
      commandLineArgsMap.put(parameter, argument);
    }
  }

  private static List<Integer> getLatenciesFromWriter(OpenCSVWriter writer) {
    List<Integer> latencies = new ArrayList<>();
    for (String[] dataRow: writer.getDataRows()) {
      latencies.add(Integer.valueOf(dataRow[2]));
    }
    return latencies;
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

  private static int findMaxLatency(OpenCSVWriter writer, String requestType) {
    int maxLatency = Integer.MIN_VALUE;
    for (String[] dataRow: writer.getDataRows()) {
      String currentRequestType = dataRow[1];
      int currentLatency = Integer.valueOf(dataRow[2]);
      if (currentRequestType.equals(requestType) && currentLatency > maxLatency) {
        maxLatency = currentLatency;
      }
    }
    return maxLatency;
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
}