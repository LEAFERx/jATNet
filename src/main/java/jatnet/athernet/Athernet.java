package jatnet.athernet;

import jatnet.mac.Mac;
import jatnet.mac.MacFrame;

import java.util.ArrayList;
import java.util.Arrays;

public class Athernet {
  private final Mac mac;

  private final AthernetAddress addr;
  private final int macFramePayloadSize;

  private boolean started = false;

  Athernet(AthernetAddress addr, Mac mac) {
    this.addr = addr;
    this.mac = mac;
    this.macFramePayloadSize = mac.getFramePayloadSize();
  }

  public void start() throws InterruptedException {
    if (started) {
      return;
    }
    started = true;
    mac.start();
    Thread.sleep(1000);
  }

  public void stop() {
    if (!started) {
      return;
    }
    mac.stop();
    started = false;
  }

  public boolean send(int dest, AthernetPacket packet) throws InterruptedException {
    byte[] data = packet.toBytes();
    int frameNums = (int) Math.ceil((double) data.length / macFramePayloadSize);
    for (int i = 0; i < frameNums - 1; i++) {
      if (!mac.write(dest, Arrays.copyOfRange(data, i * macFramePayloadSize, (i + 1) * macFramePayloadSize))) {
        return false;
      }
    }
    return mac.write(
        dest,
        Arrays.copyOfRange(data, (frameNums - 1) * macFramePayloadSize, frameNums * macFramePayloadSize),
        MacFrame.REQUIRE_ACK | MacFrame.DATA_END
    );
  }

  public boolean send(int dest, AthernetAddress dstAddr, byte[] data) throws InterruptedException {
    return send(dest, new AthernetPacket(addr, dstAddr, data));
  }

  public AthernetPacket receive() throws InterruptedException {
    ArrayList<byte[]> buffer = new ArrayList<>();
    while (true) {
      MacFrame frame = mac.read();
      assert frame.data.length == macFramePayloadSize;
      buffer.add(frame.data);
      if (frame.dataEnd()) {
        break;
      }
    }
    byte[] data = new byte[buffer.size() * macFramePayloadSize];
    for (int i = 0; i < buffer.size(); i++) {
      System.arraycopy(buffer.get(i), 0, data, i * macFramePayloadSize, macFramePayloadSize);
    }
    return AthernetPacket.parse(data);
  }
}
