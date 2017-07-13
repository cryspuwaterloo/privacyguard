package com.PrivacyGuard.Utilities;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by Near on 15-02-03.
 */
public class ByteArrayPool {
  private ConcurrentSkipListSet<ByteArray> unused, used;
  private int byteArraySize;
  private int total;

  public ByteArrayPool(int initSize, int arraySize) {
    unused = new ConcurrentSkipListSet<ByteArray>();
    byteArraySize = arraySize;
    total = initSize;
    for(int i = 0; i < initSize; i ++) {
      unused.add(new ByteArray(arraySize, i));
    }
    used = new ConcurrentSkipListSet<ByteArray>();
  }

  public ByteArray getByteArray(byte[] data, int len) {
    ByteArray ret;
    //Logger.d("Pool get", "" + unused.size() + " " + used.size());
    if(unused.isEmpty()) {
      ret = new ByteArray(byteArraySize, total ++);
    } else {
      ret =  unused.pollFirst();
    }
    ret.setData(data, len);
    used.add(ret);
    return ret;
  }

  public void release(ByteArray b) {
    //Logger.d("Pool release before", "" + unused.size() + " " + used.size());
    used.remove(b);
    b.release();
    unused.add(b);
    //Logger.d("Pool release after", "" + unused.size() + " " + used.size());
  }
}
