package jatnet.checkpoints.project3;

import jatnet.physical.PhysicalUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class Task2Cp2InternetNode {
  public static void main(String[] args) throws IOException, InterruptedException {
    InetAddress server = InetAddress.getByName("localhost");

    DatagramSocket socket = new DatagramSocket();
    socket.setSoTimeout(1000);
    socket.connect(server, 8991);

    String messageString = IOUtils.resourceToString("project3/INPUT.txt", StandardCharsets.UTF_8, PhysicalUtils.class.getClassLoader());
    String[] messages = messageString.split("\n");

    for (String msg : messages) {
      byte[] data = msg.getBytes(StandardCharsets.UTF_8);
      DatagramPacket packet = new DatagramPacket(data, data.length);
      socket.send(packet);
      Thread.sleep(1000);
    }

    socket.close();
  }
}
