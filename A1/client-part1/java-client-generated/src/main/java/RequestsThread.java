import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RequestsThread implements Runnable {
  private static final int SKI_DAY_MINS_LENGTH = 420;
  private static final int POST_REQUESTS = 100;
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
  private OpenCSVWriter csvWriter;

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
                        int phase) {
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
  }

  @Override
  public void run() {
    ApiClient client = this.apiInstance.getApiClient();
    client.setBasePath(serverAddress);

    Logger logger = LogManager.getRootLogger();

    for (int j=0; j < POST_REQUESTS; j++) {
      int liftTime = ThreadLocalRandom.current().nextInt(this.timeStart, this.timeEnd + 1);
      int skierIdForPost = ThreadLocalRandom.current().nextInt(this.skierIdsStart, this.getSkierIdsEnd + 1);
      int liftId = ThreadLocalRandom.current().nextInt(1, 30 + 1);

      LiftRide liftRide = new LiftRide();
      liftRide.setResortID("Mission Ridge");
      liftRide.setDayID("1");
      liftRide.setSkierID(String.valueOf(skierIdForPost));
      liftRide.setTime(String.valueOf(liftTime));
      liftRide.setLiftID(String.valueOf(liftId));

      try {
        this.apiInstance.writeNewLiftRide(liftRide);
        this.requestsCompleted.inc();
      } catch (ApiException e) {
        this.requestsFailed.inc();
        logger.error("POST request failed in " + String.valueOf(this.phase));
      }
    }
    for (int k=0; k < this.getRequestsNum; k++) {
      try {
        this.apiInstance.getSkierDayVertical("resortid", "dayid", "skierid");
        this.requestsCompleted.inc();
      } catch(ApiException e) {
        this.requestsFailed.inc();
        logger.error("GET request failed in " + String.valueOf(this.phase));
      }
    }
    if (this.phase != 3) {
      this.partialPhaseCompleted.countDown();
    }
    this.allPhasesCompleted.countDown();
  }
}
