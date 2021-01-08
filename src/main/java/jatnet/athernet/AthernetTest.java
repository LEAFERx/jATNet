package jatnet.athernet;

import jatnet.mac.Mac;

import java.util.Arrays;

public class AthernetTest {
  public static void main(String[] args) {
    System.out.println("Athernet Test");

    if (args.length < 1) {
      System.out.println("No argument provided");
      return;
    }

    int frameSize = 64;
    int txMacAddr = 0;
    AthernetAddress txAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 1});
    int rxMacAddr = 1;
    AthernetAddress rxAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 2});

    try {
      if (args[0].equals("tx")) {
        Mac mac = new Mac(txMacAddr, frameSize, 200, 5);
        Athernet net = new Athernet(txAddr, mac);
        net.start();

        for (int i = 0; i < 25; i++) {
          byte[] data = new byte[frameSize * 2];
          for (int j = 0; j < frameSize * 2; j++) {
            data[j] = (byte) (i + j);
          }
          boolean res = net.send(rxMacAddr, rxAddr, data);
          if (!res) {
            System.out.println("Failed to write at round " + i);
            break;
          } else {
            System.out.println("Successfully write at round " + i);
          }
        }

        Thread.sleep(1000);

        net.stop();
      } else if (args[0].equals("rx")) {
        Mac mac = new Mac(1, frameSize, 100, 5);
        Athernet net = new Athernet(rxAddr, mac);
        net.start();

        Thread.sleep(1000);

        for (int i = 0; i < 25; i++) {
          AthernetPacket packet = net.receive();
          System.out.println("Got frame at round " + i);
          System.out.println("DstAddr " + Arrays.toString(packet.getDstAddr().toBytes()));
          System.out.println("SrcAddr " + Arrays.toString(packet.getSrcAddr().toBytes()));
          System.out.println("Length " + packet.getLength());
          System.out.println("Payload " + Arrays.toString(packet.getPayload()));
        }

        Thread.sleep(1000);

        net.stop();
      } else {
        System.out.println("Unknown arg " + args[0]);
      }
    } catch (InterruptedException ignored) {
    }
  }
}
