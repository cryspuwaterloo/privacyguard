package ca.uwaterloo.crysp.privacyguard.Application.Network.Protocol;

import ca.uwaterloo.crysp.privacyguard.Application.Network.Forwarder.TCPForwarder;
import ca.uwaterloo.crysp.privacyguard.Application.Network.Protocol.IP.IPDatagram;
import ca.uwaterloo.crysp.privacyguard.Application.Network.Protocol.TCP.TCPHeader;

/**
 * Created by y59song on 16/05/14.
 */
public class TCPConnectionInfo extends ConnectionInfo {
    private static final String TAG = "TCPConnectionInfo";
    public int seq, ack;
    public TCPConnectionInfo(IPDatagram ipDatagram) {
        super(ipDatagram);
        reset(ipDatagram);
    }

    @Override
    public void reset(IPDatagram ipDatagram) {
        super.reset(ipDatagram);
        assert(protocol == IPDatagram.TCP);
        setSeq(((TCPHeader) ipDatagram.payLoad().header()).getAck_num());
        setAck(((TCPHeader) ipDatagram.payLoad().header()).getSeq_num());
    }

    public synchronized boolean setSeq(int seq) {
        boolean changed = this.seq != seq;
        this.seq = seq;
        ((TCPHeader)responseTransHeader).setSeq_num(seq);
        return changed;
    }

    public synchronized boolean setAck(int ack) {
        this.ack = ack;
        ((TCPHeader)responseTransHeader).setAck_num(ack);
        return true;
    }

    private void setFlag(byte flag) {
        ((TCPHeader)responseTransHeader).setFlag(flag);
    }

    public void increaseSeq(int inc) {
        setSeq(seq + inc);
    }

    public void increaseAck(int inc) {
        setAck(ack + inc);
    }

    public TCPHeader getTransHeader(int inc, byte flag) {
        increaseAck(inc);
        setFlag(flag);
        return (TCPHeader)responseTransHeader;
    }

    public void setup(TCPForwarder forwarder) {
          forwarder.setup(clientAddress, clientPort, serverAddress, serverPort);
        }
    }
