package jatnet.checkpoints.project3;

import jatnet.mac.Mac;
import jatnet.mac.MacFrame;
import jatnet.mac.MacFrameType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

public class Task2Cp2Gateway {
  public static void main(String[] args) throws InterruptedException, IOException {
    int athernetNodeMacAddr = 1;
    byte[] athernetNodeIp = {(byte) 192, (byte) 168, 1, 2};
    int athernetNodePort = 1234;

    int frameSize = 60;

    DatagramSocket socket = new DatagramSocket(8991);

    Mac mac = new Mac(0, frameSize, 300, 10);
    mac.start();

    Thread.sleep(2000);

    while (true) {
      byte[] data = new byte[40];
      DatagramPacket packet = new DatagramPacket(data, data.length);
      socket.receive(packet);
      byte[] macPacket = new byte[frameSize];
      Arrays.fill(macPacket, (byte) 0);
      byte[] srcIP = packet.getAddress().getAddress();
      int srcPort = packet.getPort();
      System.arraycopy(srcIP, 0, macPacket, 0, 4);
      macPacket[4] = (byte) ((srcPort >> 8) & 0xFF);
      macPacket[5] = (byte) (srcPort & 0xFF);
      System.arraycopy(athernetNodeIp, 0, macPacket, 6, 4);
      macPacket[10] = (byte) ((athernetNodePort >> 8) & 0xFF);
      macPacket[11] = (byte) (athernetNodePort & 0xFF);
      System.arraycopy(packet.getData(), packet.getOffset(), macPacket, 12, packet.getLength());
      MacFrame frame = new MacFrame(athernetNodeMacAddr, 0, MacFrameType.DATA, MacFrame.REQUIRE_ACK, macPacket);
      mac.write(frame);
    }
  }
}
