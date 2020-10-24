public class TestDatabase {
  public static void main(String[] args) {
    LiftRideDao liftRideDao = new LiftRideDao();
    liftRideDao.createLiftRide(new LiftRide("Mt_Silver", "2", "3", "5", "500", 20));
  }
}
