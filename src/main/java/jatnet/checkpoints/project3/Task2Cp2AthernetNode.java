package jatnet.checkpoints.project3;

import jatnet.mac.Mac;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Task2Cp2AthernetNode {
  public static void main(String[] args) throws InterruptedException, UnknownHostException {
    int frameSize = 60;
    Mac mac = new Mac(1, frameSize, 300, 10);
    mac.start();

    Thread.sleep(2000);

    int i = 0;

    while (true) {
      byte[] data = mac.read().data;
      byte[] srcIP = Arrays.copyOf(data, 4);
      int srcPort = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
      byte[] destIP = Arrays.copyOfRange(data, 6, 10);
      int destPort = ((data[10] & 0xFF) << 8) | (data[11] & 0xFF);
      byte[] payload = Arrays.copyOfRange(data, 12, data.length);
      System.out.println("Count: " + (i + 1));
      System.out.println("Receive packet from ip " + InetAddress.getByAddress(srcIP) + " port " + srcPort);
      System.out.println("Destination ip " + InetAddress.getByAddress(destIP) + " port " + destPort);
      System.out.println("Raw Payload: " + Arrays.toString(payload));
      int j = 0;
      for (; j < payload.length; j++) {
        if (payload[j] == 0) {
          break;
        }
      }
      String s = new String(payload, 0, j, StandardCharsets.UTF_8);
      System.out.println("String Payload: " + s);
      i += 1;
    }
  }
}
