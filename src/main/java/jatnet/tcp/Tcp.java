package jatnet.tcp;

import jatnet.athernet.Athernet;
import jatnet.athernet.AthernetAddress;
import jatnet.athernet.AthernetPacket;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.TcpPacket;

import java.net.UnknownHostException;
import java.util.Arrays;
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
    while (!Thread.currentThread().isInterrupted()) {
      try {
        AthernetPacket athernetPacket = athernet.receive();
        System.out.println("got packet");
        byte[] rawData = athernetPacket.getPayload();
        System.out.println(Arrays.toString(rawData));
        TcpPacket tcpPacket = TcpPacket.newPacket(rawData, 0, rawData.length);
        short dstPort = tcpPacket.getHeader().getDstPort().value();
        System.out.println("dst port is " + dstPort);
        TcpSocket socket = socketMap.get(dstPort);
        if (socket != null) {
          socket.onNewPacket(athernetPacket.getSrcAddr(), tcpPacket);
        }
      } catch (InterruptedException | UnknownHostException | IllegalRawDataException e) {
        break;
      }
    }
  }
}
