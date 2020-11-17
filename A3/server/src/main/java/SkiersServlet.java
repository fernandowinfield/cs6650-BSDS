import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

public class SkiersServlet extends javax.servlet.http.HttpServlet {
  private final int GET_1_PARAMS_NUM = 5;
  private final int GET_2_PARAMS_NUM = 2;
  private final int POST_PARAMS_NUM = 1;

  private final String DAYS_PARAM = "days";
  private final String SKIERS_PARAM = "skiers";
  private final String VERTICAL_PARAM = "vertical";
  private final String LIFTRIDES_PARAM = "liftrides";

  private final String[] POST_BODY_KEYS = {"resortID", "dayID", "skierID", "time", "liftID"};

  private final String QUEUE_NAME = "postingQueue";
  private Connection connection;

  @Override
  public void init() throws ServletException {
    super.init();
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUsername("root");
    factory.setPassword("chipsNguac");
    factory.setVirtualHost("/");
    factory.setHost("ec2-100-25-170-47.compute-1.amazonaws.com");
    factory.setPort(5672);
    try {
      connection = factory.newConnection();
    } catch (Exception e) {
      e.getStackTrace();
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    try {
      connection.close();
    } catch (IOException e) {
      e.getStackTrace();
    }
  }

  /**
   * GET /skiers/*
   */
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("application/json");
    String urlPath = req.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("{\"error message\": \"missing parameters\"}");
      return;
    }

    String[] splitPath = urlPath.split("/");
    String[] urlParts = Arrays.copyOfRange(splitPath, 1, splitPath.length); // Get rid of leading empty item

    // Validate url path
    if (!isUrlValid(urlParts)) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().write("{\"error message\": \"invalid inputs\"}");
    } else {
      int totalVertical = 0;
      String resortID = "";
      LiftRideDao liftRideDao = new LiftRideDao();

      // URL path has been validated, now identify which GET endpoint is being hit
      if (urlParts[1].equals(DAYS_PARAM)) {
        resortID = urlParts[0];
        String dayID = urlParts[2];
        String skierID = urlParts[4];
        totalVertical = liftRideDao.getVertical1(resortID, dayID, skierID); // /skiers/{resortID}/days/{dayID}/skiers/{skierID}
      } else if (urlParts[1].equals(VERTICAL_PARAM)) {
        String skierID = urlParts[0];
        resortID = req.getParameter("resort");
        totalVertical = liftRideDao.getVertical2(skierID, resortID); // /skiers/{skierID}/vertical
      }
      if (totalVertical == 0) {
        res.setStatus(HttpServletResponse.SC_NO_CONTENT);
      } else {
        res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().write("{\"resortID\": \"" + resortID + "\", \"totalVert\": \"" + totalVertical + "\"}");
      }
    }
  }

  /**
   * POST /skiers/*
   */
  protected void doPost(javax.servlet.http.HttpServletRequest req,
      javax.servlet.http.HttpServletResponse res)
      throws javax.servlet.ServletException, IOException {
    res.setContentType("application/json");
    String urlPath = req.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("{\"error message\": \"missing parameters\"}");
      return;
    }

    String[] splitPath = urlPath.split("/");
    String[] urlParts = Arrays.copyOfRange(splitPath, 1, splitPath.length); // get rid of leading empty item

    // and now validate url path and return the response status code
    // (and maybe also some value if input is valid)
    if (!isUrlValid(urlParts)) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().write("{\"error message\": \"invalid URL inputs\"}");
    } else {
      BufferedReader buffRdr = req.getReader();
      StringBuilder content = new StringBuilder();
      String line;
      while( (line = buffRdr.readLine()) != null) {
        content.append(line);
      }
      String jsonString = content.toString();

      // process JSON string here and do something with it if necessary
      LinkedHashMap<String, String> jsonMap = jsonStringToMap(jsonString);
      if (!validatePostBody(jsonMap)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write("{\"error message\": \"invalid body content\"}");
      } else {
        // Prepare message that will be sent to RabbitMQ
        String resortID = jsonMap.get("resortID");
        String dayID = jsonMap.get("dayID");
        String skierID = jsonMap.get("skierID");
        String time = jsonMap.get("time");
        String liftID = jsonMap.get("liftID");
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);

        String message = String.join("-", resortID, dayID, skierID, time, liftID);

        // Write lift ride to RabbitMQ's queue as a message
        channel.basicPublish("", QUEUE_NAME,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            message.getBytes("UTF-8"));
//        System.out.println(" [x] Sent '" + message + "'");

        // Send response status
        res.setStatus(HttpServletResponse.SC_CREATED);

        // Close RabbitMQ channel
        try {
          channel.close();
        } catch (TimeoutException e) {
          System.out.println("Something went wrong closing a RabbitMQ channel");
          e.getStackTrace();
        }
      }
    }
  }

  private boolean isUrlValid(String[] urlParts) {
    // urlPath  = "/1/seasons/2019/day/1/skier/123"
    // urlParts = [1, seasons, 2019, day, 1, skier, 123]

    if (urlParts.length == GET_1_PARAMS_NUM) { // Case 1: GET 1
      return areGet1ParamsValid(
          urlParts[0],
          urlParts[1],
          urlParts[2],
          urlParts[3],
          urlParts[4]
      );
    } else if (urlParts.length == GET_2_PARAMS_NUM) { // Case 2: GET 2
      return areGet2ParamsValid(
          urlParts[0],
          urlParts[1]
          );
    } else if (urlParts.length == POST_PARAMS_NUM) { // Case 3: POST
      return arePostParamsValid(urlParts[0]);
    }
    return false;
  }

  private boolean areGet1ParamsValid(String param1,
                                     String param2,
                                     String param3,
                                     String param4,
                                     String param5) {
    return (!param1.equals("") & !param3.equals("") & !param5.equals(""))
        && (param2.equals(DAYS_PARAM) && param4.equals(SKIERS_PARAM));
  }

  private boolean areGet2ParamsValid(String param1,
                                     String param2) {
    return (!param1.equals("")) && (param2.equals(VERTICAL_PARAM));
  }

  private boolean arePostParamsValid(String param1) {
    return (param1.equals(LIFTRIDES_PARAM));
  }

  private LinkedHashMap<String, String> jsonStringToMap(String jsonString) {
    String withoutBrackets = jsonString.substring(1, jsonString.length()-1).trim();
    String[] keyValueStringPairs = withoutBrackets.split(",");
    LinkedHashMap<String, String> jsonMap = new LinkedHashMap<String, String>();

    for (int i=0; i<keyValueStringPairs.length; i++) {
      String key = stripKey(keyValueStringPairs[i]);
      String value = stripValue(keyValueStringPairs[i].trim());
      jsonMap.put(key, value);
    }

    return jsonMap;
  }

  private String stripKey(String keyValueString) {
    String keyWithQuotations = keyValueString.split(":")[0].trim();
    return keyWithQuotations.substring(1, keyWithQuotations.length()-1);
  }

  private String stripValue(String keyValueString) {
    String valueWithQuotations = keyValueString.split(":")[1].trim();
    return valueWithQuotations.substring(1, valueWithQuotations.length()-1);
  }

  private boolean validatePostBody(LinkedHashMap<String, String> jsonMap) {
    Set<String> keySet = jsonMap.keySet();
    int i = 0;
    for(String key : keySet){
      if (!key.equals(POST_BODY_KEYS[i]) || jsonMap.get(key).equals("")) {
        return false;
      }
      i++;
    }
    return true;
  }
}
