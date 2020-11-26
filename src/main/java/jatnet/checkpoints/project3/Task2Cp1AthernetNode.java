package jatnet.checkpoints.project3;

import jatnet.mac.Mac;
import jatnet.mac.MacFrame;
import jatnet.mac.MacFrameType;
import jatnet.physical.PhysicalUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Task2Cp1AthernetNode {
  public static void main(String[] args) throws InterruptedException, IOException {
    int frameSize = 60;
    Mac mac = new Mac(1, frameSize, 200, 10);
    mac.start();

    Thread.sleep(2000);

    byte[] srcIP = {(byte) 192, (byte) 168, 1, 2};
    int srcPort = 1234;

    byte[] destIP = {10, 19, 74, 32};
    int destPort = 8123;

    String messageString = IOUtils.resourceToString("project3/INPUT.txt", StandardCharsets.UTF_8, PhysicalUtils.class.getClassLoader());
    String[] messages = messageString.split("\n");

    for (String msg : messages) {
      byte[] data = new byte[frameSize];
      Arrays.fill(data, (byte) 0);
      System.arraycopy(srcIP, 0, data, 0, 4);
      data[4] = (byte) ((srcPort >> 8) & 0xFF);
      data[5] = (byte) (srcPort & 0xFF);
      System.arraycopy(destIP, 0, data, 6, 4);
      data[10] = (byte) ((destPort >> 8) & 0xFF);
      data[11] = (byte) (destPort & 0xFF);
      byte[] payload = msg.getBytes(StandardCharsets.UTF_8);
      System.out.println("payload length " + payload.length);
      System.arraycopy(payload, 0, data, 12, payload.length);
      MacFrame frame = new MacFrame(0, 1, MacFrameType.DATA, MacFrame.REQUIRE_ACK, data);
      boolean res = mac.write(frame);
      Thread.sleep(1000);
    }

    mac.stop();
  }
}
