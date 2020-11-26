package jatnet.checkpoints.project3;

import jatnet.mac.Mac;
import jatnet.mac.MacFrame;
import jatnet.mac.MacFrameType;
import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;
import org.pcap4j.util.MacAddress;
import org.pcap4j.util.NifSelector;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Task3Gateway {
  public static void main(String[] args) throws InterruptedException, PcapNativeException, NotOpenException, UnknownHostException {
    PcapNetworkInterface nif;
    try {
      nif = new NifSelector().selectNetworkInterface();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    if (nif == null) {
      return;
    }

    System.out.println(nif.getName() + "(" + nif.getDescription() + ")");

    final Inet4Address address = (Inet4Address) nif.getAddresses().get(nif.getAddresses().size() - 1).getAddress();
    System.out.println("ipv4 address is " + address);
    final MacAddress MAC_ADDR = MacAddress.getByAddress(nif.getLinkLayerAddresses().get(0).getAddress());
    final MacAddress GATEWAY_MAC_ADDR = MacAddress.getByName("00-00-5e-00-01-01", "-");
    // final MacAddress GATEWAY_MAC_ADDR = MacAddress.getByName("76-1b-4d-ca-a1-34", "-");
    System.out.println("MAC address is " + MAC_ADDR);

    final PcapHandle captureHandle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
    final PcapHandle sendHandle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
    captureHandle.setFilter("icmp or arp", BpfProgram.BpfCompileMode.OPTIMIZE);

    UnknownPacket.Builder payloadBuilder = new UnknownPacket.Builder();
    IcmpV4EchoPacket.Builder icmpV4EchoBuilder = new IcmpV4EchoPacket.Builder();
    icmpV4EchoBuilder.payloadBuilder(payloadBuilder);
    IcmpV4CommonPacket.Builder icmpV4CommonBuilder = new IcmpV4CommonPacket.Builder();
    icmpV4CommonBuilder
        .type(IcmpV4Type.ECHO)
        .code(IcmpV4Code.NO_CODE)
        .payloadBuilder(icmpV4EchoBuilder)
        .correctChecksumAtBuild(true);
    IpV4Packet.Builder ipv4Builder = new IpV4Packet.Builder();
    ipv4Builder
        .srcAddr(address)
        .version(IpVersion.IPV4)
        .tos(IpV4Rfc791Tos.newInstance((byte) 0))
        .ttl((byte) 100)
        .protocol(IpNumber.ICMPV4)
        .identification((short) 100)
        .payloadBuilder(icmpV4CommonBuilder)
        .correctChecksumAtBuild(true)
        .correctLengthAtBuild(true);
    EthernetPacket.Builder ethernetBuilder = new EthernetPacket.Builder();
    ethernetBuilder
        .srcAddr(MAC_ADDR)
        .dstAddr(GATEWAY_MAC_ADDR)
        .type(EtherType.IPV4)
        .payloadBuilder(ipv4Builder)
        .paddingAtBuild(true);

    final LinkedBlockingQueue<IpV4Packet> pingResult = new LinkedBlockingQueue<>();

    final PacketListener listener = packet -> {
      if (packet.contains(IcmpV4EchoReplyPacket.class)) {
        try {
          System.out.println("got a reply");
          pingResult.put(packet.get(IpV4Packet.class));
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    };

    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(() -> {
      while (true) {
        try {
          sendHandle.loop(-1, listener);
        } catch (PcapNativeException e) {
          e.printStackTrace();
        } catch (InterruptedException | NotOpenException e) {
          break;
        }
      }
    });

    Thread.sleep(2000);

    payloadBuilder.rawData(new byte[]{1, 2, 3, 4});
    ipv4Builder.dstAddr(address);
    ethernetBuilder.build();


    int athernetNodeMacAddr = 1;

    int frameSize = 20;

    Mac mac = new Mac(0, frameSize, 1000, 10);
    mac.start();

    Thread.sleep(2000);

    System.out.println("Athernet start");

    while (true) {
      byte[] data = mac.read().data;
      System.out.println(Arrays.toString(data));
      byte[] srcIP = Arrays.copyOf(data, 4);
      byte[] dstIP = Arrays.copyOfRange(data, 4, 8);
      int id = ((data[8] & 0xFF) << 8) | (data[9] & 0xFF);
      byte[] payload = Arrays.copyOfRange(data, 10, 20);
      System.out.println("payload " + Arrays.toString(payload));
      System.out.println((Inet4Address) InetAddress.getByAddress(dstIP));
      payloadBuilder.rawData(payload);
      ipv4Builder.dstAddr((Inet4Address) InetAddress.getByAddress(dstIP));
      icmpV4EchoBuilder.sequenceNumber((short) id);
      EthernetPacket packet = ethernetBuilder.build();
      sendHandle.sendPacket(packet);
      IpV4Packet replyPacket = pingResult.take();
      byte[] replyData = new byte[frameSize];
      System.arraycopy(dstIP, 0, replyData, 0, 4);
      System.arraycopy(srcIP, 0, replyData, 4, 4);
      replyData[8] = data[8];
      replyData[9] = data[9];
      byte[] replyPayload = replyPacket.get(UnknownPacket.class).getRawData();
      System.out.println("reply payload " + Arrays.toString(replyPayload));
      System.out.println("reply payload string " + new String(replyPayload, 0, 10, StandardCharsets.UTF_8));
      System.arraycopy(replyPayload, 0, replyData, 10, 10);
      System.out.println("reply " + Arrays.toString(replyData));
      MacFrame frame = new MacFrame(athernetNodeMacAddr, 0, MacFrameType.DATA, MacFrame.REQUIRE_ACK, replyData);
      System.out.println(mac.write(frame));
    }
  }
}
