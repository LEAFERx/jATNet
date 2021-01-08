package jatnet.mac;

import java.util.Arrays;

public class MacFrame {
  public static final int REQUIRE_ACK = 1;
  public static final int DATA_END = 2;

  public final int dest;
  public final int src;
  public final MacFrameType type;
  public final int flags;
  public final byte[] data;

  public MacFrame(int dest, int src, MacFrameType type, int flags, byte[] data) {
    assert dest < 0xF;
    assert src < 0xF;
    this.dest = dest;
    this.src = src;
    this.type = type;
    this.flags = flags;
    this.data = data;
  }

  public static MacFrame parseFrame(byte[] frame) {
    int dest = frame[0] >> 4;
    int src = frame[0] & 0xF;
    MacFrameType type = MacFrameType.valueOf(frame[1] >> 4);
    int flags = frame[1] & 0xF;
    byte[] data = Arrays.copyOfRange(frame, 2, frame.length);
    return new MacFrame(dest, src, type, flags, data);
  }

  public boolean requireACK() {
    return (flags & REQUIRE_ACK) != 0;
  }

  public boolean dataEnd() {
    return (flags & DATA_END) != 0;
  }

  public byte[] toBytes() {
    byte[] result = new byte[data.length + 2];
    result[0] = (byte) (dest << 4 | src);
    result[1] = (byte) (type.getValue() << 4 | flags);
    System.arraycopy(data, 0, result, 2, data.length);
    return result;
  }
}
