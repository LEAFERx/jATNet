package jatnet.checkpoints.project3;

import jatnet.mac.Mac;
import jatnet.mac.MacFrame;
import jatnet.mac.MacFrameType;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Task3AthernetNode {
  public static void main(String[] args) throws InterruptedException, UnknownHostException {
    final byte[] srcIP = {(byte) 192, (byte) 168, 1, 2};
    // final byte[] dstIP = {119, 75, (byte) 217, 26};
    final byte[] dstIP = {(byte) 10, (byte) 20, (byte) 93, (byte) 61};

    int frameSize = 20;

    Mac mac = new Mac(1, frameSize, 300, 10);
    mac.start();

    for (int i = 0; i < 10; i++) {
      byte[] data = new byte[frameSize];
      System.arraycopy(srcIP, 0, data, 0, 4);
      System.arraycopy(dstIP, 0, data, 4, 4);
      data[8] = (byte) ((i >> 8) & 0xFF);
      data[9] = (byte) (i & 0xFF);
      byte[] payload = "Athernet!!".getBytes(StandardCharsets.UTF_8);
      System.arraycopy(payload, 0, data, 10, 10);
      MacFrame frame = new MacFrame(0, 1, MacFrameType.DATA, MacFrame.REQUIRE_ACK, data);
      long time = System.nanoTime();
      mac.write(frame);
      byte[] replyData = mac.read().data;
      byte[] replySrcIP = Arrays.copyOf(replyData, 4);
      byte[] replyDstIP = Arrays.copyOfRange(replyData, 4, 8);
      byte[] replyPayload = Arrays.copyOfRange(replyData, 10, 10);
      System.out.println("Receive ICMP Echo Reply from ip " + InetAddress.getByAddress(replySrcIP));
      System.out.println("Destination ip " + InetAddress.getByAddress(replyDstIP));
      // System.out.println("Raw payload " + Arrays.toString(replyPayload));
      // int j = 0;
      // for (; j < replyPayload.length; j++) {
      //   if (replyPayload[j] == 0) {
      //     break;
      //   }
      // }
      // System.out.println("String payload " + new String(replyPayload, 0, j, StandardCharsets.UTF_8));
      double ping = (System.nanoTime() - time) / 1000000.0;
      System.out.println("Ping " + ping + "ms");
      Thread.sleep(1000);
    }

    mac.stop();
  }
}
