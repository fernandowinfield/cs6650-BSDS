import java.util.concurrent.CountDownLatch;

/**
 * Inspired on code by
 * @author igortn
 */
public class RequestCounterBarrier {
  private int count = 0;

  synchronized public void inc() {
    count++;
  }

  public int getVal() {
    return this.count;
  }
}