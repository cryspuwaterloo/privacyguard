package ca.uwaterloo.crysp.privacyguard.Application.Network.Protocol.IP;

import ca.uwaterloo.crysp.privacyguard.Utilities.ByteOperations;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by y59song on 28/05/14.
 */
public class IPv4Header extends IPHeader {
    public IPv4Header(byte[] data) {
        lengthIndex = 2;
        srcIndex = 12;
        dstIndex = 16;
        addressSize = 4;
        headerLength = (data[0] & 0xFF) % 16 * 4;
        length = ((data[lengthIndex] & 0xFF) << 8) + (data[lengthIndex + 1] & 0xFF);
        protocol = data[9];

        try {
            srcAddress = InetAddress.getByAddress(Arrays.copyOfRange(data, srcIndex, srcIndex + addressSize));
            dstAddress = InetAddress.getByAddress(Arrays.copyOfRange(data, dstIndex, dstIndex + addressSize));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        checkSum_pos = 10;
        checkSum_size = 2;
        this.data = Arrays.copyOfRange(data, 0, headerLength);
    }


    @Override
    public byte[] getPseudoHeader(int dataLength) {
        return ByteOperations.concatenate(
                getSrcAddressByteArray(),
                getDstAddressByteArray(),
                new byte[]{0, protocol(), (byte) ((dataLength & 0xFF00) >> 8), (byte) (dataLength & 0xFF)}
        );
    }
}

