package jatnet.checkpoints.project4;

import jatnet.athernet.Athernet;
import jatnet.athernet.AthernetAddress;
import jatnet.athernet.AthernetPacket;
import jatnet.mac.Mac;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Task1AthernetNode {
  public Athernet athernet;

  public static void main(String[] args) throws UnknownHostException, InterruptedException {
    int frameSize = 64;
    int gatewayMacAddr = 0;
    int nodeMacAddr = 1;
    short nodePort = 18992;
    AthernetAddress gatewayAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 1});
    AthernetAddress nodeAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 2});
    InetAddress dstAddr = InetAddress.getByAddress(new byte[]{(byte) 90, (byte) 130, (byte) 70, 73});
    // InetAddress dstAddr = InetAddress.getByAddress(new byte[]{(byte) 202, 120, (byte) 58, (byte) 157});

    Mac mac = new Mac(nodeMacAddr, frameSize, 1000, 10);
    Athernet athernet = new Athernet(nodeAddr, mac);
    athernet.start();
    Thread.sleep(2000);
    Task1AthernetNode node = new Task1AthernetNode();
    node.athernet = athernet;
    Thread recThread = new Thread(new ReceiveThread(node));
    recThread.start();
    System.out.println("Athernet Start");

    Scanner scanner = new Scanner(System.in);

    while (true) {
      String msg = scanner.nextLine();
      if (msg != null) {
        msg = msg.trim();
        if (msg.length() > 0) {
          byte[] data = msg.getBytes(StandardCharsets.UTF_8);
          if (!athernet.send(gatewayMacAddr, gatewayAddr, data)) {
            System.out.println("Athernet send failed!");
          }
        }
      } else {
        Thread.yield();
      }
    }
  }

  private static class ReceiveThread implements Runnable {
    private final Task1AthernetNode node;

    ReceiveThread(Task1AthernetNode node) {
      this.node = node;
    }

    @Override
    public void run() {
      try {
        while (true) {
          AthernetPacket ap = node.athernet.receive();
          String msg = new String(ap.getPayload(), StandardCharsets.UTF_8);
          if (msg.startsWith("D")) {
            System.out.println("Message from data link: " + msg.substring(1));
          } else if (msg.startsWith("C")) {
            System.out.println("Message from command link: " + msg.substring(1));
          } else if (msg.equals("END")) {
            System.out.println("Data link end!");
          }
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
