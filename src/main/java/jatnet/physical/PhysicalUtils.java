package jatnet.physical;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PhysicalUtils {
  private static boolean loaded = false;

  private static float[] carrier = null;
  private static float[] preamble = null;

  public static boolean load() {
    if (carrier == null) {
      try {
        String carrierString = IOUtils.resourceToString("carrier.txt", StandardCharsets.UTF_8, PhysicalUtils.class.getClassLoader());
        String[] carrierStringSplit = carrierString.split(" ");
        carrier = new float[carrierStringSplit.length];
        for (int i = 0; i < carrierStringSplit.length; i++) {
          carrier[i] = Float.parseFloat(carrierStringSplit[i]);
        }
      } catch (IOException e) {
        return false;
      }
    }
    if (preamble == null) {
      try {
        String preambleString = IOUtils.resourceToString("preamble.txt", StandardCharsets.UTF_8, PhysicalUtils.class.getClassLoader());
        String[] preambleStringSplit = preambleString.split(" ");
        preamble = new float[preambleStringSplit.length];
        for (int i = 0; i < preambleStringSplit.length; i++) {
          preamble[i] = Float.parseFloat(preambleStringSplit[i]);
        }
      } catch (IOException e) {
        return false;
      }
    }
    loaded = true;
    return true;
  }

  public static boolean isLoaded() {
    return loaded;
  }

  public static float[] getCarrier() {
    return carrier;
  }

  public static float[] getPreamble() {
    return preamble;
  }

}
