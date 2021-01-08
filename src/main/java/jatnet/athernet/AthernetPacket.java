package jatnet.athernet;

// Packet Header Format (in bytes):
// 0~3 SrcAddr
// 4~7 DstAddr
// 8~9 Length

import java.util.Arrays;

public class AthernetPacket {
  private final AthernetAddress srcAddr;
  private final AthernetAddress dstAddr;
  private final short length;
  private final byte[] payload;

  AthernetPacket(AthernetAddress srcAddr, AthernetAddress dstAddr, byte[] payload) {
    this.srcAddr = srcAddr;
    this.dstAddr = dstAddr;
    this.length = (short) (payload.length + 10);
    this.payload = payload;
  }

  public static AthernetPacket parse(byte[] data) {
    AthernetAddress srcAddr = new AthernetAddress(Arrays.copyOf(data, 4));
    AthernetAddress dstAddr = new AthernetAddress(Arrays.copyOfRange(data, 4, 8));
    short length = (short) (data[8] << 8 | data[9]);
    return new AthernetPacket(srcAddr, dstAddr, Arrays.copyOfRange(data, 10, length));
  }

  public AthernetAddress getSrcAddr() {
    return srcAddr;
  }

  public AthernetAddress getDstAddr() {
    return dstAddr;
  }

  public short getLength() {
    return length;
  }

  public byte[] getPayload() {
    return payload;
  }

  public byte[] toBytes() {
    byte[] data = new byte[length];
    System.arraycopy(srcAddr.toBytes(), 0, data, 0, 4);
    System.arraycopy(dstAddr.toBytes(), 0, data, 4, 4);
    data[8] = (byte) (length >> 8);
    data[9] = (byte) (length & 0xFF);
    System.arraycopy(payload, 0, data, 10, length - 10);
    return data;
  }
}
