package jatnet.mac;

import java.util.HashMap;
import java.util.Map;

public enum MacFrameType {
  DATA(0),
  ACK(1);

  private static final Map<Integer, MacFrameType> map = new HashMap<>();

  static {
    for (MacFrameType macFrameType : MacFrameType.values()) {
      map.put(macFrameType.value, macFrameType);
    }
  }

  private final int value;

  MacFrameType(int value) {
    this.value = value;
  }

  public static MacFrameType valueOf(int value) {
    return map.get(value);
  }

  public int getValue() {
    return value;
  }
}
