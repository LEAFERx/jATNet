package jatnet.mac;

import java.util.Arrays;

public class MacTest {
  public static void main(String[] args) {
    System.out.println("Mac Test");

    if (args.length < 1) {
      System.out.println("No argument provided");
      return;
    }

    int frameSize = 250;

    try {
      if (args[0].equals("tx")) {
        Mac mac = new Mac(0, frameSize, 200, 5);
        mac.start();

        Thread.sleep(1000);

        for (int i = 0; i < 25; i++) {
          byte[] data = new byte[frameSize];
          for (int j = 0; j < frameSize; j++) {
            data[j] = (byte) (i + j);
          }
          boolean res = mac.write(1, data);
          if (!res) {
            System.out.println("Failed to write at round " + i);
            break;
          } else {
            System.out.println("Successfully write at round " + i);
          }
        }

        Thread.sleep(1000);

        mac.stop();
      } else if (args[0].equals("rx")) {
        Mac mac = new Mac(1, frameSize, 100, 5);
        mac.start();

        Thread.sleep(1000);

        for (int i = 0; i < 25; i++) {
          MacFrame frame = mac.read();
          System.out.println("Got frame at round " + i);
          System.out.println("Dest " + frame.dest);
          System.out.println("Src " + frame.src);
          System.out.println("Data " + Arrays.toString(frame.data));
        }

        Thread.sleep(1000);

        mac.stop();
      } else {
        System.out.println("Unknown arg " + args[0]);
      }
    } catch (InterruptedException ignored) {
    }
  }
}
