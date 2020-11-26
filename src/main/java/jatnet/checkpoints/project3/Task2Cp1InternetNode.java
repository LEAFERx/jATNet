package jatnet.checkpoints.project3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Task2Cp1InternetNode {
  public static void main(String[] args) throws IOException {
    byte[] buffer = new byte[1024];

    DatagramSocket socket = new DatagramSocket(8123);

    for (int i = 0; i < 30; i++) {
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
      socket.receive(packet);
      System.out.println("Count: " + (i + 1));
      System.out.println("Receive packet from ip " + packet.getAddress() + " port " + packet.getPort());
      System.out.println("Raw Payload: " + Arrays.toString(packet.getData()));
      String s = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
      System.out.println("String Payload: " + s);
    }

    socket.close();
  }
}
