public class LiftRide {
  private String resortID;
  private String dayID;
  private String skierID;
  private String time;
  private String liftID;
  private int vertical;

  public LiftRide(String resortID,
                  String dayID,
                  String skierID,
                  String time,
                  String liftID,
                  int vertical) {
    this.resortID = resortID;
    this.dayID = dayID;
    this.skierID = skierID;
    this.time = time;
    this.liftID = liftID;
    this.vertical = vertical;
  }

  public String getResortID() {
    return resortID;
  }

  public void setResortID(String resortID) {
    this.resortID = resortID;
  }

  public String getDayID() {
    return dayID;
  }

  public void setDayID(String dayID) {
    this.dayID = dayID;
  }

  public String getSkierID() {
    return skierID;
  }

  public void setSkierID(String skierID) {
    this.skierID = skierID;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public String getLiftID() {
    return liftID;
  }

  public void setLiftID(String liftID) {
    this.liftID = liftID;
  }

  public int getVertical() {
    return vertical;
  }

  public void setVertical(int vertical) {
    this.vertical = vertical;
  }
}
