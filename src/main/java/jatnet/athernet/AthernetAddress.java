package jatnet.athernet;

public class AthernetAddress {
  private final byte[] addr;

  AthernetAddress(byte[] addr) {
    assert addr.length == 4;
    this.addr = addr;
  }

  public byte[] toBytes() {
    return addr;
  }
}
