package ca.uwaterloo.crysp.privacyguard.Utilities;

/**
 * Created by frank on 2014-03-27.
 */
public class ByteOperations {
  public static byte[] concatenate(byte[]...arrays) {
    int totalLength = 0;
    for(byte[] array : arrays)
      if(array != null) totalLength += array.length;
    byte[] result = new byte[totalLength];

    int currentIndex = 0;
    for(byte[] array : arrays) {
      if(array == null) continue;
      System.arraycopy(array, 0, result, currentIndex, array.length);
      currentIndex += array.length;
    }
    return result;
  }

  public static void swap(byte[] array, int pos1, int pos2, int length) {
    for(int i = 0; i < length; i ++) {
      byte temp = array[pos1 + i];
      array[pos1 + i] = array[pos2 + i];
      array[pos2 + i] = temp;
    }
  }

  public static int byteArrayToInteger(byte[] array, int start, int end) {
    int ret = 0;
    for(int i = start; i < end; i ++)
      ret = (ret << 8) + (array[i] & 0xFF);
    return ret;
  }

  public static String byteArrayToHexString(byte[] array) {
    String ret = "";
    for(byte b : array) {
      ret += Integer.toHexString(b & 0xFF) + " ";
    }
    return ret;
  }

  public static String byteArrayToString(byte[] array) {
    return byteArrayToString(array, 0, array.length);
    //return new String(array);
  }

  public static String byteArrayToString(byte[] array, int start, int end) {
    String ret = "";
    for(int i = start; i < end; i ++)
      ret += Character.toString((char) (array[i] & 0xFF));
    return ret;
  }

  public static byte[] byteArrayAppend(byte[] array, int length) {
    if(array.length >= length) return array;
    byte[] ret = new byte[length];
    for(int i = 0; i < length; i ++) {
      if(i < array.length) ret[i] = array[i];
      else ret[i] = 0;
    }
    return ret;
  }

  public static byte[] computeCheckSum(byte[] data) {
    int result = 0;
    if(data.length % 2 != 0) result = ((data[data.length - 1] & 0xFF) << 8);
    for(int i = 0; i < data.length / 2; i ++)
      result += ((data[2 * i] & 0xFF) << 8) + (data[2 * i + 1] & 0xFF);
    while((result >> 16) > 0) {
      int carry = result >> 16;
      result &= 0xFFFF;
      result += carry;
    }
    result = ~result;
    return new byte[]{(byte)((result >> 8) & 0xFF), (byte)(result & 0xFF)};
  }
}
