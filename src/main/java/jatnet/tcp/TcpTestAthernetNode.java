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
    short nodePort = 18976;
    AthernetAddress gatewayAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 1});
    AthernetAddress nodeAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 2});
    InetAddress dstAddr = InetAddress.getByAddress(new byte[]{10, 15, 45, 53});

    Mac mac = new Mac(nodeMacAddr, frameSize, 200, 10);
    Athernet athernet = new Athernet(nodeAddr, mac);
    Tcp tcp = new Tcp(athernet);
    tcp.start();
    TcpSocket socket = new TcpSocket(tcp, nodePort);
    if (socket.connect(gatewayMacAddr, dstAddr, (short) 80)) {
      if (socket.send("GET /".getBytes(StandardCharsets.UTF_8))) {
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
