package jatnet.checkpoints.project4;

import jatnet.athernet.Athernet;
import jatnet.athernet.AthernetAddress;
import jatnet.athernet.AthernetPacket;
import jatnet.mac.Mac;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class Task1Gateway {
  public LinkedBlockingQueue<byte[]> msgToSend = new LinkedBlockingQueue<>();
  public Socket dataSocket = null;
  public Socket commSocket = null;
  public Athernet athernet = null;

  public int nodeMacAddr;
  public AthernetAddress nodeAddr;

  public InetAddress dstAddr;

  public static void main(String[] args) throws IOException, InterruptedException {
    int frameSize = 64;
    int gatewayMacAddr = 0;
    int nodeMacAddr = 1;
    short nodePort = 18992;
    AthernetAddress gatewayAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 1});
    AthernetAddress nodeAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 2});
    InetAddress dstAddr = InetAddress.getByAddress(new byte[]{(byte) 90, (byte) 130, (byte) 70, 73});
    // InetAddress dstAddr = InetAddress.getByAddress(new byte[]{(byte) 202, 120, (byte) 58, (byte) 157});

    Mac mac = new Mac(gatewayMacAddr, frameSize, 1000, 20);
    Athernet athernet = new Athernet(gatewayAddr, mac);
    athernet.start();
    Thread.sleep(2000);
    System.out.println("Athernet Start!");

    Task1Gateway gateway = new Task1Gateway();
    gateway.nodeMacAddr = nodeMacAddr;
    gateway.nodeAddr = nodeAddr;
    gateway.dstAddr = dstAddr;
    gateway.athernet = athernet;

    Thread commThread = new Thread(new CommThread(gateway));
    Thread dataThread = new Thread(new DataThread(gateway));
    Thread sendThread = new Thread(new SendThread(gateway));
    commThread.start();
    dataThread.start();
    sendThread.start();

    OutputStream commOutput = null;
    OutputStream dataOutput = null;

    while (true) {
      AthernetPacket ap = athernet.receive();
      String msg = new String(ap.getPayload(), StandardCharsets.UTF_8);
      msg = msg.trim();
      if (gateway.commSocket == null) {
        if (msg.startsWith("CONN ")) {
          String host = msg.substring(5);
          gateway.commSocket = new Socket(host, 21);
          commOutput = gateway.commSocket.getOutputStream();
        }
      } else {
        if (commOutput != null) {
          msg = msg + "\r\n";
          commOutput.write(msg.getBytes(StandardCharsets.UTF_8));
          commOutput.flush();
        }
      }
    }
  }

  private static class CommThread implements Runnable {
    private final Task1Gateway gateway;

    CommThread(Task1Gateway gateway) {
      this.gateway = gateway;
    }

    @Override
    public void run() {
      while (gateway.commSocket == null) {
        Thread.yield();
      }
      try {
        Thread.sleep(1000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(gateway.commSocket.getInputStream()));
        while (true) {
          String msg = reader.readLine().trim();
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
            short port = (short) (Integer.parseInt(numbers[4]) * 256 + Integer.parseInt(numbers[5]));
            gateway.dataSocket = new Socket(gateway.dstAddr, port);
          }
          msg = "C" + msg;
          System.out.println("From command" + msg);
          gateway.msgToSend.put(msg.getBytes(StandardCharsets.UTF_8));
        }
      } catch (InterruptedException | IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static class DataThread implements Runnable {
    private final Task1Gateway gateway;

    DataThread(Task1Gateway gateway) {
      this.gateway = gateway;
    }

    @Override
    public void run() {
      while (gateway.dataSocket == null) {
        Thread.yield();
      }
      try {
        boolean sendEnd = false;
        Thread.sleep(1000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(gateway.dataSocket.getInputStream()));
        while (true) {
          String msg = reader.readLine();
          if (msg == null) {
            if (!sendEnd) {
              System.out.println("From data end");
              gateway.msgToSend.put("END".getBytes(StandardCharsets.UTF_8));
              sendEnd = true;
            }
          } else {
            sendEnd = false;
            msg = "D" + msg.trim();
            System.out.println("From data" + msg);
            gateway.msgToSend.put(msg.getBytes(StandardCharsets.UTF_8));
          }
        }
      } catch (InterruptedException | IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static class SendThread implements Runnable {
    private final Task1Gateway gateway;

    SendThread(Task1Gateway gateway) {
      this.gateway = gateway;
    }

    @Override
    public void run() {
      try {
        while (gateway.athernet == null) {
          Thread.yield();
        }
        while (true) {
          byte[] msg = gateway.msgToSend.take();
          gateway.athernet.send(gateway.nodeMacAddr, gateway.nodeAddr, msg);
        }
      } catch (InterruptedException ignored) {

      }
    }
  }
}
