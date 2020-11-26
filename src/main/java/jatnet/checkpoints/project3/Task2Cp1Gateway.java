package jatnet.checkpoints.project3;

import jatnet.mac.Mac;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

// 0~3 srcIP
// 4~5 srcPort
// 6~9 destIP
// 10~11 destPort


public class Task2Cp1Gateway {
  public static void main(String[] args) throws IOException, InterruptedException {
    // Map<NatKey, DatagramSocket> nat = new HashMap<>();

    DatagramSocket socket = new DatagramSocket();

    Mac mac = new Mac(0, 60, 200, 10);
    mac.start();

    Thread.sleep(2000);

    int globalPortCount = 8991;

    while (true) {
      byte[] macPacket = mac.read().data;
      byte[] srcIP = Arrays.copyOf(macPacket, 4);
      int srcPort = ((macPacket[4] & 0xFF) << 8) | (macPacket[5] & 0xFF);
      byte[] destIP = Arrays.copyOfRange(macPacket, 6, 10);
      int destPort = ((macPacket[10] & 0xFF) << 8) | (macPacket[11] & 0xFF);
      byte[] data = Arrays.copyOfRange(macPacket, 12, macPacket.length);
      System.out.println("src ip " + Arrays.toString(srcIP));
      System.out.println("src port " + srcPort);
      System.out.println("dest ip " + Arrays.toString(destIP));
      System.out.println("dest port " + destPort);
      System.out.println("data " + Arrays.toString(data));
      NatKey key = new NatKey(InetAddress.getByAddress(srcIP), srcPort);
//      DatagramSocket socket = nat.get(key);
//      if (socket == null) {
//        socket = new DatagramSocket(globalPortCount);
//        globalPortCount += 1;
//        socket.setSoTimeout(1000);
//        nat.put(key, socket);
//      }
      DatagramPacket packet = new DatagramPacket(data, data.length);
      socket.connect(InetAddress.getByAddress(destIP), destPort);
      socket.send(packet);
    }
  }

  public static class NatKey {
    public final InetAddress addr;
    public final int port;

    public NatKey(InetAddress addr, int port) {
      this.addr = addr;
      this.port = port;
    }
  }
}
