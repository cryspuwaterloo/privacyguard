package ca.uwaterloo.crysp.privacyguard.Application.Network.Protocol.IP;

import ca.uwaterloo.crysp.privacyguard.Utilities.ByteOperations;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by y59song on 28/05/14.
 */
public class IPv6Header extends IPHeader {
  public IPv6Header(byte[] data) {
    lengthIndex = 4;
    srcIndex = 8;
    dstIndex = 24;
    addressSize = 16;

    headerLength = 40; //TODO
    length = ((data[lengthIndex] & 0xFF) << 8) + (data[lengthIndex + 1] & 0xFF);
    protocol = data[6];

    try {
      srcAddress = InetAddress.getByAddress(Arrays.copyOfRange(data, srcIndex, srcIndex + addressSize));
      dstAddress = InetAddress.getByAddress(Arrays.copyOfRange(data, dstIndex, dstIndex + addressSize));
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    checkSum_pos = -1;
    checkSum_size = -1;
    this.data = Arrays.copyOfRange(data, 0, headerLength);
  }

  @Override
  public byte[] getPseudoHeader(int dataLength) {
    return ByteOperations.concatenate(
      getSrcAddressByteArray(),
      getDstAddressByteArray(),
      new byte[]{
        0, 0, (byte)((dataLength & 0xFF00) >> 8), (byte)(dataLength & 0xFF),
        0, 0, 0, protocol()
      }
    );
  }
}
