package jatnet.tcp;

import jatnet.athernet.AthernetAddress;
import org.pcap4j.packet.TcpMaximumSegmentSizeOption;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UnknownPacket;
import org.pcap4j.packet.namednumber.TcpPort;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class TcpSocket {
  private final Tcp tcp;
  private final short srcPort;
  private final LinkedBlockingQueue<byte[]> buffer = new LinkedBlockingQueue<>();
  private final LinkedBlockingQueue<TcpPacket> sendBuffer = new LinkedBlockingQueue<>();

  private final Thread sendThread;

  int dest;
  private State state = State.CLOSED;
  private short dstPort;
  private Inet4Address address;
  private AthernetAddress athernetAddress;
  private int sequence = 1955;
  private int acknowledgement;
  private int acked = 0;

  public TcpSocket(Tcp tcp, short port) {
    this.tcp = tcp;
    this.srcPort = port;
    this.sendThread = new Thread(new SendThread(this));
    this.sendThread.start();
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
    if (header.getAck()) {
      System.out.println("got an ack with acked " + header.getAcknowledgmentNumber());
      acked = header.getAcknowledgmentNumber();
    }
    switch (state) {
      case CLOSED:
        // Drop
        break;
      case SYN_SENT:
        if (header.getSyn() && header.getAck()) {
          acknowledgement = header.getSequenceNumber() + 1;
          sendAck();
          state = State.ESTABLISHED;
        }
        break;
      case ESTABLISHED:
        acknowledgement = header.getSequenceNumber();
        if (header.getSyn() || header.getFin()) {
          acknowledgement += 1;
        }
        if (packet.getPayload() != null) {
          byte[] rawData = packet.getPayload().getRawData();
          System.out.println("Got data " + Arrays.toString(rawData));
          acknowledgement += rawData.length;
          buffer.put(rawData);
        }
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
        .window((short) 65535)
        .ack(true)
        .psh(true)
        .correctChecksumAtBuild(true)
        .correctLengthAtBuild(true);
    TcpPacket packet = builder.build();
    sendBuffer.put(packet);
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
        .window((short) 65535)
        .correctChecksumAtBuild(true)
        .correctLengthAtBuild(true);
    TcpPacket packet = builder.build();
    sendBuffer.put(packet);
  }

  private void sendAck() throws InterruptedException, UnknownHostException {
    sendAck(new byte[]{});
  }

  private void sendSyn() throws InterruptedException, UnknownHostException {
    UnknownPacket.Builder ub = new UnknownPacket.Builder();
    ub.rawData(new byte[]{});
    TcpPacket.Builder builder = new TcpPacket.Builder();
    ArrayList<TcpPacket.TcpOption> options = new ArrayList<>();
    TcpMaximumSegmentSizeOption.Builder mssBuilder = new TcpMaximumSegmentSizeOption.Builder();
    mssBuilder.maxSegSize((short) 1460).length((byte) 4).correctLengthAtBuild(true);
    options.add(mssBuilder.build());
    builder
        .payloadBuilder(ub)
        .srcAddr(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}))
        .dstAddr(address)
        .srcPort(new TcpPort(srcPort, "srcPort"))
        .dstPort(new TcpPort(dstPort, "dstPort"))
        .sequenceNumber(sequence)
        .acknowledgmentNumber(0)
        .dataOffset((byte) 20)
        .syn(true)
        .window((short) 65535)
        // .options(options)
        .correctChecksumAtBuild(true)
        .correctLengthAtBuild(true);
    TcpPacket packet = builder.build();
    sequence += 1;
    sendBuffer.put(packet);
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
        .acknowledgmentNumber(0)
        .dataOffset((byte) 20)
        .fin(true)
        .window((short) 65535)
        .correctChecksumAtBuild(true)
        .correctLengthAtBuild(true);
    TcpPacket packet = builder.build();
    sequence += 1;
    sendBuffer.put(packet);
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

  private static class SendThread implements Runnable {
    private final TcpSocket socket;

    SendThread(TcpSocket socket) {
      this.socket = socket;
    }

    @Override
    public void run() {
      try {
        while (true) {
          TcpPacket packet = socket.sendBuffer.take();
          while (!socket.tcp.send(socket.dest, socket.athernetAddress, packet)) {
            System.out.println("TCP send failed! Retrying...");
            Thread.yield();
          }
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
