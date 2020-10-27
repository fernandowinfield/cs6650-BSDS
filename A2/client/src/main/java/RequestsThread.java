import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RequestsThread implements Runnable {
  private static final int SKI_DAY_MINS_LENGTH = 420;
  private static final int POST_REQUESTS = 1000;
  private static final int GET_REQUESTS_PHASE_1 = 5;
  private static final int GET_REQUESTS_PHASE_2 = 5;
  private static final int GET_REQUESTS_PHASE_3 = 10;

  private int skierIdsStart;
  private int getSkierIdsEnd;
  private int timeStart;
  private int timeEnd;
  private String serverAddress;
  private SkiersApi apiInstance;
  private RequestCounterBarrier requestsCompleted;
  private RequestCounterBarrier requestsFailed;
  private CountDownLatch partialPhaseCompleted;
  private CountDownLatch allPhasesCompleted;
  private int phase;
  private int getRequestsNum;
  private BlockingQueue<String> queue;
  private String resortID;
  private String dayID;
  private int numLifts;

  public RequestsThread(int skierIdsStart,
                        int skierIdsEnd,
                        int timeStart,
                        int timeEnd,
                        String serverAddress,
                        SkiersApi apiInstance,
                        RequestCounterBarrier requestsCompleted,
                        RequestCounterBarrier requestsFailed,
                        CountDownLatch partialPhaseCompleted,
                        CountDownLatch allPhasesCompleted,
                        int phase,
                        BlockingQueue<String> queue,
                        String resortID,
                        String dayID,
                        int numLifts) {
    this.skierIdsStart = skierIdsStart;
    this.getSkierIdsEnd = skierIdsEnd;
    this.timeStart = timeStart;
    this.timeEnd = timeEnd;
    this.serverAddress = serverAddress;
    this.apiInstance = apiInstance;
    this.requestsCompleted = requestsCompleted;
    this.requestsFailed = requestsFailed;
    this.partialPhaseCompleted = partialPhaseCompleted;
    this.allPhasesCompleted = allPhasesCompleted;
    this.phase = phase;
    switch(phase) {
      case 1:
        this.getRequestsNum = GET_REQUESTS_PHASE_1;
        break;
      case 2:
        this.getRequestsNum = GET_REQUESTS_PHASE_2;
        break;
      case 3:
        this.getRequestsNum = GET_REQUESTS_PHASE_3;
        break;
      default:
        this.getRequestsNum = GET_REQUESTS_PHASE_1;
    }
    this.queue = queue;
    this.resortID = resortID;
    this.dayID = dayID;
    this.numLifts = numLifts;
  }

  @Override
  public void run() {
    ApiClient client = this.apiInstance.getApiClient();
    client.setBasePath(serverAddress);
//    client.setConnectTimeout(20000);

    Logger logger = LogManager.getRootLogger();

    // POST requests
    for (int j=0; j < POST_REQUESTS; j++) {
      int liftTime = ThreadLocalRandom.current().nextInt(this.timeStart, this.timeEnd + 1);
      int skierIdForPost = ThreadLocalRandom.current().nextInt(this.skierIdsStart, this.getSkierIdsEnd + 1);
      int liftId = ThreadLocalRandom.current().nextInt(5, this.numLifts + 1);

      LiftRide liftRide = new LiftRide();
      liftRide.setResortID(this.resortID);
      liftRide.setDayID(this.dayID);
      liftRide.setSkierID(String.valueOf(skierIdForPost));
      liftRide.setTime(String.valueOf(liftTime));
      liftRide.setLiftID(String.valueOf(liftId));

      long requestStart = System.currentTimeMillis();

      try {
        this.apiInstance.writeNewLiftRide(liftRide);
        long requestEnd = System.currentTimeMillis();
        this.requestsCompleted.inc();
        long latency = requestEnd - requestStart;
        String requestDataRow = String.valueOf(requestStart) + "," + "POST" + "," + String.valueOf(latency) + "," + "201" + "\n";
//        String requestDataRow = String.valueOf(this.requestsCompleted.getVal()) + "," + String.valueOf(requestStart) + "," + "POST" + "," + String.valueOf(latency) + "," + "201" + "\n";
        try {
          this.queue.put(requestDataRow);
        } catch (InterruptedException e) {
          System.out.println("Something went wrong while putting POST request data row into blocking queue");
        }
      } catch (ApiException e) {
        this.requestsFailed.inc();
        logger.error("POST request failed in PHASE " + String.valueOf(this.phase));
        e.printStackTrace();
      }
    }
    // GET1 requests
    for (int k=0; k < this.getRequestsNum; k++) {
      String skierIdForGET1 = String.valueOf(ThreadLocalRandom.current().nextInt(this.skierIdsStart, this.getSkierIdsEnd + 1));
      long requestStart = System.currentTimeMillis();

      try {
        this.apiInstance.getSkierDayVertical(this.resortID, this.dayID, skierIdForGET1);
        long requestEnd = System.currentTimeMillis();
        this.requestsCompleted.inc();
        long latency = requestEnd - requestStart;
        String requestDataRow = String.valueOf(requestStart) + "," + "GET1" + "," + String.valueOf(latency) + "," + "200" + "\n";
//        String requestDataRow = String.valueOf(this.requestsCompleted.getVal()) + "," + String.valueOf(requestStart) + "," + "GET1" + "," + String.valueOf(latency) + "," + "200" + "\n";

        try {
          this.queue.put(requestDataRow);
        } catch (InterruptedException e) {
          System.out.println("Something went wrong while putting GET1 request data row into blocking queue");
        }
      } catch(ApiException e) {
        this.requestsFailed.inc();
        logger.error("GET1 request failed in PHASE " + String.valueOf(this.phase));
      }
    }
    // Only call GET2 API on phase 3
    if (this.phase == 3) {
      // GET2 requests
      for (int k=0; k < this.getRequestsNum; k++) {
        String skierIdForGET2 = String.valueOf(ThreadLocalRandom.current().nextInt(this.skierIdsStart, this.getSkierIdsEnd + 1));
        List<String> resortIdForGET2 = new ArrayList<>();
        resortIdForGET2.add(this.resortID);
        long requestStart = System.currentTimeMillis();

        try {
          this.apiInstance.getSkierResortTotals(skierIdForGET2, resortIdForGET2);
          long requestEnd = System.currentTimeMillis();
          this.requestsCompleted.inc();
          long latency = requestEnd - requestStart;
          String requestDataRow = String.valueOf(requestStart) + "," + "GET2" + "," + String.valueOf(latency) + "," + "200" + "\n";
//        String requestDataRow = String.valueOf(this.requestsCompleted.getVal()) + "," + String.valueOf(requestStart) + "," + "GET2" + "," + String.valueOf(latency) + "," + "200" + "\n";

          try {
            this.queue.put(requestDataRow);
          } catch (InterruptedException e) {
            System.out.println("Something went wrong while putting GET2 request data row into blocking queue");
          }
        } catch(ApiException e) {
          this.requestsFailed.inc();
          logger.error("GET2 request failed in PHASE " + String.valueOf(this.phase));
        }
      }
    }
    // Phase 3 is the last phase, so only count down the "partial" latch on phases that are not 3
    if (this.phase != 3) {
      this.partialPhaseCompleted.countDown();
    }
    // Always count down the "allPhases" latch as the thread comes to an end
    this.allPhasesCompleted.countDown();
  }
}
