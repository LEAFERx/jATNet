package jatnet.tcp;

import jatnet.athernet.Athernet;
import jatnet.athernet.AthernetAddress;
import jatnet.mac.Mac;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class TcpTestAthernetNode {
  int frameSize = 64;
  int gatewayMacAddr = 0;
  int nodeMacAddr = 1;
  short commPort = 18980;
  short dataPort = 18999;
  AthernetAddress gatewayAddr;
  AthernetAddress nodeAddr;
  InetAddress dstAddr;
  Mac mac;
  Athernet athernet;
  Tcp tcp;
  TcpSocket commSocket;
  TcpSocket dataSocket;

  public static void main(String[] args) throws InterruptedException, UnknownHostException {
    TcpTestAthernetNode node = new TcpTestAthernetNode();
    node.gatewayAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 1});
    node.nodeAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 2});
    node.dstAddr = InetAddress.getByAddress(new byte[]{(byte) 90, (byte) 130, (byte) 70, 73});
    // node.dstAddr = InetAddress.getByAddress(new byte[]{(byte) 202, 120, (byte) 58, (byte) 157});

    node.mac = new Mac(node.nodeMacAddr, node.frameSize, 400, 20);
    node.athernet = new Athernet(node.nodeAddr, node.mac);
    node.tcp = new Tcp(node.athernet);
    node.tcp.start();

    Thread commThread = new Thread(new CommThread(node));
    commThread.start();

    node.commSocket = new TcpSocket(node.tcp, node.commPort);
    if (node.commSocket.connect(node.gatewayMacAddr, node.dstAddr, (short) 21)) {
      Thread.sleep(2000);
      Scanner scanner = new Scanner(System.in);
      while (true) {
        String msg = scanner.nextLine();
        if (msg != null) {
          msg = msg.trim();
          if (msg.length() > 0) {
            node.commSocket.send((msg + "\r\n").getBytes(StandardCharsets.UTF_8));
          }
        }
      }
    } else {
      System.out.println("Failed to connect.");
    }
    node.tcp.stop();
  }

  private static class CommThread implements Runnable {
    private final TcpTestAthernetNode node;

    CommThread(TcpTestAthernetNode node) {
      this.node = node;
    }

    @Override
    public void run() {
      try {
        while (true) {
          byte[] data = node.commSocket.read();
          if (data != null) {
            String msg = new String(data, StandardCharsets.UTF_8);
            msg = msg.trim();
            if (msg.startsWith("227 Entering Passive Mode")) {
              int left = msg.indexOf('(');
              int right = msg.indexOf(')');
              String addr = msg.substring(left + 1, right);
              String[] numbers = addr.split(",");
              if (numbers.length != 6) {
                System.out.println("Got a bad passive reply! ");
                System.out.println("Raw msg " + msg);
                System.out.println("Parsed addr" + addr);
                System.out.println("Numbers " + Arrays.toString(numbers));
              }
              byte[] ip = new byte[]{
                  (byte) Integer.parseInt(numbers[0]),
                  (byte) Integer.parseInt(numbers[1]),
                  (byte) Integer.parseInt(numbers[2]),
                  (byte) Integer.parseInt(numbers[3]),
              };
              InetAddress pasvAddr = InetAddress.getByAddress(ip);
              int port = Integer.parseInt(numbers[4]) * 256 + Integer.parseInt(numbers[5]);
              node.dataSocket = new TcpSocket(node.tcp, node.dataPort);
              node.dataSocket.connect(node.gatewayMacAddr, pasvAddr, (short) port);
            }
            msg = "C" + msg;
            System.out.println("From command " + msg);
          }
        }
      } catch (InterruptedException | UnknownHostException ignored) {
      }
    }
  }
}
