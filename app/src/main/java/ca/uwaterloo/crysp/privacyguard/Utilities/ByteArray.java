package ca.uwaterloo.crysp.privacyguard.Utilities;

import java.util.Comparator;

/**
 * Created by Near on 15-02-03.
 */
public class ByteArray implements Comparable<ByteArray> {
  private byte[] data;
  private int length, capacity;
  private int id;
  private boolean inUse;

  protected ByteArray(int capacity, int id) {
    data = new byte[capacity];
    this.capacity = capacity;
    inUse = false;
    this.id = id;
  }

  public void setData(byte[] d, int len) {
    length = len;
    inUse = true;
    System.arraycopy(d, 0, data, 0, len);
  }

  public byte[] data() {
    return data;
  }

  public int length() {
    return length;
  }

  public boolean isInUse() {
    return inUse;
  }

  public void release() {
    inUse = false;
  }

  @Override
  public int compareTo(ByteArray another) {
    return id - another.id;
  }
}
