package jatnet.tcp;

import jatnet.athernet.Athernet;
import jatnet.athernet.AthernetAddress;
import jatnet.mac.Mac;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TcpTestAthernetNode {
  public static void main(String[] args) throws InterruptedException, UnknownHostException {
    int frameSize = 64;
    int gatewayMacAddr = 0;
    int nodeMacAddr = 1;
    short nodePort = 18992;
    AthernetAddress gatewayAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 1});
    AthernetAddress nodeAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 2});
    InetAddress dstAddr = InetAddress.getByAddress(new byte[]{(byte) 90, (byte) 130, (byte) 70, 73});
    // InetAddress dstAddr = InetAddress.getByAddress(new byte[]{(byte) 202, 120, (byte) 58, (byte) 157});

    Mac mac = new Mac(nodeMacAddr, frameSize, 400, 20);
    Athernet athernet = new Athernet(nodeAddr, mac);
    Tcp tcp = new Tcp(athernet);
    tcp.start();
    TcpSocket socket = new TcpSocket(tcp, nodePort);
    if (socket.connect(gatewayMacAddr, dstAddr, (short) 21)) {
      Thread.sleep(2000);
      if (socket.send("USER anonymous\r\n".getBytes(StandardCharsets.UTF_8))) {
        socket.send("PASS a@a.com\r\n".getBytes(StandardCharsets.UTF_8));
        socket.send("PWD\r\n".getBytes(StandardCharsets.UTF_8));
        byte[] data = socket.read();
        if (data != null) {
          System.out.println("Got data " + Arrays.toString(data));
        }
      } else {
        System.out.println("Failed to send");
      }
      socket.close();
    } else {
      System.out.println("Failed to connect.");
    }
    tcp.stop();
  }
}
