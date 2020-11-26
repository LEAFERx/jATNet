package jatnet.physical;

public class Modem {
  public static float[] modulate(byte[] frame, int bitWidth) {
    float[] preamble = PhysicalUtils.getPreamble();
    float[] carrier = PhysicalUtils.getCarrier();

    int waveLength = preamble.length + frame.length * 8 * bitWidth;
    float[] wave = new float[waveLength];

    int waveIndex = 0;
    for (float p : preamble) {
      wave[waveIndex++] = p;
    }

    for (byte b : frame) {
      for (int i = 7; i >= 0; i--) {
        int bit = (b >> i) & 1;
        if (bit == 0) {
          for (int j = 0; j < bitWidth; j++) {
            wave[waveIndex] = -carrier[waveIndex - preamble.length];
            waveIndex += 1;
          }
        } else {
          for (int j = 0; j < bitWidth; j++) {
            wave[waveIndex] = carrier[waveIndex - preamble.length];
            waveIndex += 1;
          }
        }
      }
    }
    return wave;
  }

  public static byte[] demodulate(float[] data, int bitWidth) {
    float[] carrier = PhysicalUtils.getCarrier();

    float[] corr = new float[data.length];
    for (int i = 0; i < data.length; i++) {
      corr[i] = data[i] * carrier[i];
    }

    byte[] frame = new byte[data.length / 8 / bitWidth];
    for (int i = 0; i < frame.length; i++) {
      int b = 0;
      for (int j = 0; j < 8; j++) {
        b = b << 1;
        float power = 0;
        for (int k = 1; k < bitWidth - 1; k++) {
          power += corr[bitWidth * (8 * i + j) + k];
        }
        if (power > 0) {
          b += 1;
        }
      }
      frame[i] = (byte) b;
    }

    return frame;
  }
}
