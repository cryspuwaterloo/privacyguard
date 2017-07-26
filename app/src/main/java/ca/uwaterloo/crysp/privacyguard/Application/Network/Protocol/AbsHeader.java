package ca.uwaterloo.crysp.privacyguard.Application.Network.Protocol;

/**
 * Created by frank on 2014-03-27.
 */
public abstract class AbsHeader {
  protected byte[] data;
  protected int checkSum_pos, checkSum_size;
  public abstract AbsHeader reverse();
  public int headerLength() {
    if(data == null) return 0;
    else return data.length;
  }
  public void setCheckSum(byte[] checksum) {
    if(checkSum_pos < 0 || checkSum_size < 0) return;
    System.arraycopy(checksum, 0, data, checkSum_pos, checkSum_size);
  };
  public byte[] toByteArray() {
    return data;
  }
}
