package jatnet.tcp;

import jatnet.athernet.AthernetAddress;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UnknownPacket;
import org.pcap4j.packet.namednumber.TcpPort;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

public class TcpSocket {
  private final Tcp tcp;
  private final short srcPort;
  private final LinkedBlockingQueue<byte[]> buffer = new LinkedBlockingQueue<>();

  int dest;
  private State state;
  private short dstPort;
  private Inet4Address address;
  private AthernetAddress athernetAddress;
  private int sequence = 0;
  private int acknowledgement;
  private int acked = 0;

  public TcpSocket(Tcp tcp, short port) {
    this.tcp = tcp;
    this.srcPort = port;
    tcp.register(port, this);
  }

  public boolean connect(int dest, InetAddress address, short port) throws UnknownHostException, InterruptedException {
    if (state != State.CLOSED) {
      return false;
    }
    this.dest = dest;
    this.dstPort = port;
    this.address = (Inet4Address) address;
    this.athernetAddress = new AthernetAddress(address.getAddress());
    state = State.SYN_SENT;
    sendSyn();
    while (state != State.ESTABLISHED) {
      Thread.yield();
    }
    return true;
  }

  public boolean close() throws UnknownHostException, InterruptedException {
    if (state != State.ESTABLISHED && state != State.CLOSE_WAIT) {
      return false;
    }
    if (state == State.ESTABLISHED) {
      state = State.FIN_WAIT_1;
    } else {
      state = State.LAST_ACK;
    }
    sendFin();
    while (state != State.CLOSED) {
      Thread.yield();
    }
    return true;
  }

  public void onNewPacket(AthernetAddress srcAddr, TcpPacket packet) throws UnknownHostException, InterruptedException {
    TcpPacket.TcpHeader header = packet.getHeader();
    if (header.getSyn() || header.getFin()) {
      acknowledgement += 1;
    }
    switch (state) {
      case CLOSED:
        // Drop
        break;
      case SYN_SENT:
        if (header.getSyn() && header.getAck()) {
          sendAck();
          state = State.ESTABLISHED;
        }
        break;
      case ESTABLISHED:
        buffer.put(packet.getPayload().getRawData());
        sendAck();
        if (header.getFin()) {
          state = State.CLOSE_WAIT;
        }
        break;
      case FIN_WAIT_1:
        if (header.getAck() && header.getFin()) {
          sendAck();
          state = State.CLOSED;
        } else if (header.getAck()) {
          state = State.FIN_WAIT_2;
        } else if (header.getFin()) {
          state = State.CLOSING;
        }
        break;
      case FIN_WAIT_2:
        if (header.getFin()) {
          sendAck();
          state = State.CLOSED;
        }
        break;
      case CLOSING:
      case LAST_ACK:
        if (header.getAck()) {
          state = State.CLOSED;
        }
        break;
      default:
        break;
    }
    if (header.getAck()) {
      acked = header.getAcknowledgmentNumber();
    }
  }

  public byte[] read() throws InterruptedException {
    if (state != State.ESTABLISHED) {
      return null;
    }
    return buffer.take();
  }

  public boolean send(byte[] data) throws InterruptedException, UnknownHostException {
    if (state != State.ESTABLISHED) {
      return false;
    }
    sendData(data);
    sequence += data.length;
    waitAck();
    return true;
  }

  private void waitAck() {
    while (acked != sequence) {
      Thread.yield();
    }
  }

  private void sendData(byte[] data) throws InterruptedException, UnknownHostException {
    UnknownPacket.Builder ub = new UnknownPacket.Builder();
    ub.rawData(data);
    TcpPacket.Builder builder = new TcpPacket.Builder();
    builder
        .payloadBuilder(ub)
        .srcAddr(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}))
        .dstAddr(address)
        .srcPort(new TcpPort(srcPort, "srcPort"))
        .dstPort(new TcpPort(dstPort, "dstPort"))
        .sequenceNumber(sequence)
        .acknowledgmentNumber(acknowledgement)
        .dataOffset((byte) 20)
        .window((short) 1000)
        .correctChecksumAtBuild(true)
        .correctLengthAtBuild(true);
    TcpPacket packet = builder.build();
    if (!tcp.send(dest, athernetAddress, packet)) {
      throw new InterruptedException();
    }
  }

  private void sendAck(byte[] data) throws InterruptedException, UnknownHostException {
    UnknownPacket.Builder ub = new UnknownPacket.Builder();
    ub.rawData(data);
    TcpPacket.Builder builder = new TcpPacket.Builder();
    builder
        .payloadBuilder(ub)
        .srcAddr(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}))
        .dstAddr(address)
        .srcPort(new TcpPort(srcPort, "srcPort"))
        .dstPort(new TcpPort(dstPort, "dstPort"))
        .sequenceNumber(sequence)
        .acknowledgmentNumber(acknowledgement)
        .dataOffset((byte) 20)
        .ack(true)
        .window((short) 1000)
        .correctChecksumAtBuild(true)
        .correctLengthAtBuild(true);
    TcpPacket packet = builder.build();
    if (!tcp.send(dest, athernetAddress, packet)) {
      throw new InterruptedException();
    }
  }

  private void sendAck() throws InterruptedException, UnknownHostException {
    sendAck(new byte[]{});
  }

  private void sendSyn() throws InterruptedException, UnknownHostException {
    UnknownPacket.Builder ub = new UnknownPacket.Builder();
    ub.rawData(new byte[]{});
    TcpPacket.Builder builder = new TcpPacket.Builder();
    builder
        .payloadBuilder(ub)
        .srcAddr(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}))
        .dstAddr(address)
        .srcPort(new TcpPort(srcPort, "srcPort"))
        .dstPort(new TcpPort(dstPort, "dstPort"))
        .sequenceNumber(sequence)
        .acknowledgmentNumber(acknowledgement)
        .dataOffset((byte) 20)
        .syn(true)
        .window((short) 1000)
        .correctChecksumAtBuild(true)
        .correctLengthAtBuild(true);
    TcpPacket packet = builder.build();
    sequence += 1;
    if (!tcp.send(dest, athernetAddress, packet)) {
      throw new InterruptedException();
    }
  }

  private void sendFin() throws InterruptedException, UnknownHostException {
    UnknownPacket.Builder ub = new UnknownPacket.Builder();
    ub.rawData(new byte[]{});
    TcpPacket.Builder builder = new TcpPacket.Builder();
    builder
        .payloadBuilder(ub)
        .srcAddr(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}))
        .dstAddr(address)
        .srcPort(new TcpPort(srcPort, "srcPort"))
        .dstPort(new TcpPort(dstPort, "dstPort"))
        .sequenceNumber(sequence)
        .acknowledgmentNumber(acknowledgement)
        .dataOffset((byte) 20)
        .fin(true)
        .window((short) 1000)
        .correctChecksumAtBuild(true)
        .correctLengthAtBuild(true);
    TcpPacket packet = builder.build();
    sequence += 1;
    if (!tcp.send(dest, athernetAddress, packet)) {
      throw new InterruptedException();
    }
  }

  public enum State {
    CLOSED,
    SYN_SENT,
    ESTABLISHED,
    FIN_WAIT_1,
    FIN_WAIT_2,
    CLOSING,
    CLOSE_WAIT,
    LAST_ACK
  }
}
