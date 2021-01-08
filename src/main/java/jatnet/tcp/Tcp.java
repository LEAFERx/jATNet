package jatnet.tcp;

import jatnet.athernet.Athernet;
import jatnet.athernet.AthernetAddress;
import jatnet.athernet.AthernetPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UnknownPacket;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class Tcp implements Runnable {
  private final Athernet athernet;

  private final Thread tcpThread;

  private boolean started = false;
  private Map<Short, TcpSocket> socketMap = new HashMap<>();

  public Tcp(Athernet athernet) {
    this.athernet = athernet;
    this.tcpThread = new Thread(this);
  }

  public void register(short port, TcpSocket socket) {
    socketMap.put(port, socket);
  }

  public boolean send(int dest, AthernetAddress dstAddr, TcpPacket packet) throws InterruptedException {
    return athernet.send(dest, dstAddr, packet.getRawData());
  }

  public void start() throws InterruptedException {
    if (started) {
      return;
    }

    tcpThread.start();
    athernet.start();

    started = true;
  }

  public void stop() {
    if (!started) {
      return;
    }

    tcpThread.interrupt();
    try {
      tcpThread.join();
    } catch (InterruptedException ignored) {
    }

    athernet.stop();

    started = false;
  }

  @Override
  public void run() {
    UnknownPacket.Builder builder = new UnknownPacket.Builder();
    while (!Thread.currentThread().isInterrupted()) {
      try {
        AthernetPacket athernetPacket = athernet.receive();
        builder.rawData(athernetPacket.getPayload());
        Packet packet = builder.build();
        if (packet.contains(TcpPacket.class)) {
          TcpPacket tcpPacket = packet.get(TcpPacket.class);
          short dstPort = tcpPacket.getHeader().getDstPort().value();
          TcpSocket socket = socketMap.get(dstPort);
          if (socket != null) {
            socket.onNewPacket(athernetPacket.getSrcAddr(), tcpPacket);
          }
        }
      } catch (InterruptedException | UnknownHostException e) {
        break;
      }
    }
  }
}
