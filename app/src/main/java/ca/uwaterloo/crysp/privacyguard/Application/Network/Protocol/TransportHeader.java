package ca.uwaterloo.crysp.privacyguard.Application.Network.Protocol;

/**
 * Created by frank on 2014-03-28.
 */
public abstract class TransportHeader extends AbsHeader {
    protected int srcPort, dstPort;
    public TransportHeader(byte []data) {
        srcPort = ((data[0] & 0xFF) << 8) + (data[1] & 0xFF);
        dstPort = ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);
    }

    public TransportHeader(byte[] data, int start) {
        srcPort = ((data[0 + start] & 0xFF) << 8) + (data[1 + start] & 0xFF);
        dstPort = ((data[2 + start] & 0xFF) << 8) + (data[3 + start] & 0xFF);
    }
    @Override
    public abstract TransportHeader reverse();
    public int getSrcPort() {
        return srcPort;
    };
    public int getDstPort() {
        return dstPort;
    }

    public void updateSrcPort(int srcPort) {
        this.srcPort = srcPort;
        data[1] = (byte)(srcPort & 0xFF);
        data[0] = (byte)(((srcPort & 0xFF)) >> 8);
    }
}
