package jatnet.checkpoints.project3;

import jatnet.mac.Mac;
import jatnet.mac.MacFrame;
import jatnet.mac.MacFrameType;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Task4AthernetNode {
  public static void main(String[] args) throws InterruptedException, UnknownHostException {
    int frameSize = 20;

    Mac mac = new Mac(1, frameSize, 300, 10);
    mac.start();

    while (true) {
      byte[] echoData = mac.read().data;
      byte[] echoIp = Arrays.copyOf(echoData, 4);
      int id = ((echoData[4] & 0xFF) << 8) | (echoData[5] & 0xFF);
      byte[] echoPayload = Arrays.copyOfRange(echoData, 6, 20);
      System.out.println("Receive ICMP Echo from ip " + InetAddress.getByAddress(echoIp));
      System.out.println("Id " + id);
      System.out.println("Raw payload " + Arrays.toString(echoPayload));
      int j = 0;
      for (; j < echoPayload.length; j++) {
        if (echoPayload[j] == 0) {
          break;
        }
      }
      System.out.println("String payload " + new String(echoPayload, 0, j, StandardCharsets.UTF_8));
      MacFrame frame = new MacFrame(0, 1, MacFrameType.DATA, MacFrame.REQUIRE_ACK, echoData);
      mac.write(frame);
    }
  }
}
