package jatnet.checkpoints.project3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class Task1Tx {
  public static void main(String[] args) throws IOException, InterruptedException {
    Random rnd = new Random();

    InetAddress server = InetAddress.getByName("localhost");

    DatagramSocket socket = new DatagramSocket();
    socket.setSoTimeout(1000);
    socket.connect(server, 8123);

    for (int i = 0; i < 10; i++) {
      byte[] data = new byte[20];
      rnd.nextBytes(data);
      DatagramPacket packet = new DatagramPacket(data, data.length);
      socket.send(packet);
      Thread.sleep(1000);
    }

    socket.close();
  }
}
