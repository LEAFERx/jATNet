package jatnet.physical;

import java.util.Arrays;

public class Ping {
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("No argument provided");
      return;
    }

    byte[] pingData = new byte[1];
    Arrays.fill(pingData, (byte) 1);

    Physical phy = new Physical(3, 1);
    phy.start();

    try {
      Thread.sleep(1000);

      if (args[0].equals("tx")) {
        while (true) {
          long time = System.nanoTime();
          phy.write(pingData);
          phy.read();
          double ping = (System.nanoTime() - time) / 1000000.0;
          System.out.println("ping: " + ping + "ms");
          Thread.sleep(1000);
        }
      } else if (args[0].equals("rx")) {
        while (true) {
          phy.read();
          phy.write(pingData);
        }
      } else {
        System.out.println("Unknown arg " + args[0]);
      }
    } catch (InterruptedException ignored) {
    }
  }
}
