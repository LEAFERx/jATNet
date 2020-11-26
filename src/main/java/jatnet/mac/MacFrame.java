package jatnet.mac;

import java.util.Arrays;

public class MacFrame {
  public static final int REQUIRE_ACK = 1;

  public final int dest;
  public final int src;
  public final MacFrameType type;
  public final int flags;
  public final byte[] data;

  public MacFrame(int dest, int src, MacFrameType type, int flags, byte[] data) {
    this.dest = dest;
    this.src = src;
    this.type = type;
    this.flags = flags;
    this.data = data;
  }

  public static MacFrame parseFrame(byte[] frame) {
    int dest = frame[0];
    int src = frame[1];
    MacFrameType type = MacFrameType.valueOf(frame[2] >> 4);
    int flags = frame[2] & 0xF;
    byte[] data = Arrays.copyOfRange(frame, 3, frame.length);
    return new MacFrame(dest, src, type, flags, data);
  }

  public boolean requireACK() {
    return (flags & REQUIRE_ACK) == 1;
  }

  public byte[] toBytes() {
    byte[] result = new byte[data.length + 3];
    result[0] = (byte) dest;
    result[1] = (byte) src;
    result[2] = (byte) (type.getValue() << 4 | flags);
    System.arraycopy(data, 0, result, 3, data.length);
    return result;
  }
}
