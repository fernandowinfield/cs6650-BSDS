import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.SkiersApi;


import java.io.File;
import java.util.*;


public class SkiersApiExample {
//  private static final String BASE_URL_PATH = "http://ec2-34-224-2-148.compute-1.amazonaws.com:8080/javaserver_war";
  private static final String BASE_URL_PATH = "http://ec2-34-224-2-148.compute-1.amazonaws.com:8080/java-server_war";

  public static void main(String[] args) {

    SkiersApi apiInstance = new SkiersApi();
    ApiClient client = apiInstance.getApiClient();
    client.setBasePath(BASE_URL_PATH);


    List<String> resort = Arrays.asList("resort_example"); // List<String> | resort to query by
    List<String> dayID = Arrays.asList("dayID_example"); // List<String> | day number in the season
    try {
      // Test call to GET /skiers/{resortID}/days/{dayID}/skiers/{skierID}
      SkierVertical result = apiInstance.getSkierDayVertical("resortid", "dayid", "skierid");
      System.out.println(result);

      // Test call to GET /skiers/{skierID}/vertical
      List<String> listforApiCall = new ArrayList<String>();
      SkierVertical result2 = apiInstance.getSkierResortTotals("skier56", listforApiCall);
      System.out.println(result2);

      // Test call to POST /skiers/liftrides
      LiftRide liftRide = new LiftRide();
      liftRide.setResortID("Mission Ridge");
      liftRide.setDayID("23");
      liftRide.setSkierID("7889");
      liftRide.setTime("217");
      liftRide.setLiftID("21");
      apiInstance.writeNewLiftRide(liftRide);

    } catch (ApiException e) {
      System.err.println("Exception when calling SkiersApi#getSkierDayVertical");
      e.printStackTrace();
    }
  }
}