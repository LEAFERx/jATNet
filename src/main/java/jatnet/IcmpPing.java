package jatnet;

import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;
import org.pcap4j.util.ByteArrays;
import org.pcap4j.util.MacAddress;
import org.pcap4j.util.NifSelector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IcmpPing {

  private static final MacAddress MAC_ADDR = MacAddress.getByName("B0-52-16-09-69-5B", "-");

  private IcmpPing() {
  }

  public static void main(String[] args) throws PcapNativeException, NotOpenException {
    final Inet4Address address;
    try {
      // address = (Inet4Address) InetAddress.getLocalHost();
      address = (Inet4Address) InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 43, (byte) 229});
    } catch (UnknownHostException e) {
      return;
    }

    final Inet4Address dst;
    try {
      // dst = (Inet4Address) InetAddress.getByName("www.bing.cn");
      dst = (Inet4Address) InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 43, 72});
    } catch (UnknownHostException e) {
      return;
    }

    final IcmpV4Type type = IcmpV4Type.ECHO;

    IcmpV4Code code = IcmpV4Code.NO_CODE;

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

    System.out.println(nif.getAddresses());
    System.out.println(nif.getLinkLayerAddresses());

    final PcapHandle handle4capture = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);

    final PcapHandle handle4send = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);

    handle4capture.setFilter(
        "1 == 1",
        BpfProgram.BpfCompileMode.OPTIMIZE);

    Packet.Builder icmpV4echob = new IcmpV4EchoPacket.Builder();

    IcmpV4CommonPacket.Builder icmpV4b = new IcmpV4CommonPacket.Builder();
    icmpV4b.type(type).code(code).payloadBuilder(icmpV4echob).correctChecksumAtBuild(true);

    final IpV4Packet.Builder ipv4b = new IpV4Packet.Builder();
    ipv4b
        .version(IpVersion.IPV4)
        .tos(IpV4Rfc791Tos.newInstance((byte) 0))
        .identification((short) 100)
        .ttl((byte) 100)
        .protocol(IpNumber.ICMPV4)
        .payloadBuilder(icmpV4b)
        .correctChecksumAtBuild(true)
        .correctLengthAtBuild(true);

    final EthernetPacket.Builder eb = new EthernetPacket.Builder();
    eb.type(EtherType.IPV4).payloadBuilder(ipv4b).paddingAtBuild(true);
    eb.dstAddr(MacAddress.ETHER_BROADCAST_ADDRESS);

    final PacketListener listener =
        packet -> {
          if (packet.contains(IcmpV4EchoReplyPacket.class)) {
            System.out.println(
                "** Got reply. SRC: " + packet.get(IpV4Packet.class).getHeader().getSrcAddr() + "; DST: " + packet.get(IpV4Packet.class).getHeader().getDstAddr() + "."
            );
          } else if (packet.contains(ArpPacket.class)) {
            ArpPacket arp = packet.get(ArpPacket.class);
            if (arp.getHeader().getOperation().equals(ArpOperation.REPLY)) {
              if (arp.getHeader().getDstProtocolAddr().equals(address)) {
                System.out.println("set dst mac to " + arp.getHeader().getSrcHardwareAddr());
                eb.dstAddr(arp.getHeader().getSrcHardwareAddr());
              }
            }
          }
        };

    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(
        () -> {
          while (true) {
            try {
              handle4capture.loop(-1, listener);
            } catch (PcapNativeException e) {
              e.printStackTrace();
            } catch (InterruptedException | NotOpenException e) {
              break;
            }
          }
        });

    try {
      Thread.sleep(2000);
    } catch (InterruptedException ignored) {
    }

    ipv4b.srcAddr(address);
    ipv4b.dstAddr(dst);
    eb.srcAddr(MAC_ADDR);

    ArpPacket.Builder arpBuilder = new ArpPacket.Builder();
    arpBuilder
        .hardwareType(ArpHardwareType.ETHERNET)
        .protocolType(EtherType.IPV4)
        .hardwareAddrLength((byte) MacAddress.SIZE_IN_BYTES)
        .protocolAddrLength((byte) ByteArrays.INET4_ADDRESS_SIZE_IN_BYTES)
        .operation(ArpOperation.REQUEST)
        .srcHardwareAddr(MAC_ADDR)
        .srcProtocolAddr(address)
        .dstHardwareAddr(MacAddress.getByName("76-1b-4d-ca-a1-34", "-"))
        // .dstHardwareAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
        .dstProtocolAddr(dst);

    EthernetPacket.Builder etherBuilder = new EthernetPacket.Builder();
    etherBuilder
        .dstAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
        .srcAddr(MAC_ADDR)
        .type(EtherType.ARP)
        .payloadBuilder(arpBuilder)
        .paddingAtBuild(true);

    // eb.dstAddr(MacAddress.getByName("76-1b-4d-ca-a1-34", "-"));

    try (BufferedReader r = new BufferedReader(new InputStreamReader(System.in))) {
      while (true) {
        EthernetPacket packet = eb.build();
        if (packet.getHeader().getDstAddr().equals(MacAddress.ETHER_BROADCAST_ADDRESS)) {
          System.out.println("! Waiting for ARP resolution");
          try {
            handle4send.sendPacket(etherBuilder.build());
            Thread.sleep(1000);
          } catch (InterruptedException ignored) {
          }
        } else {
          System.out.println("Got Dst MAC: " + packet.getHeader().getDstAddr());
          System.out.println("** Hit Enter key to send echo **");
          r.readLine();
          try {
            handle4send.sendPacket(packet);
          } catch (PcapNativeException | NotOpenException e) {
            e.printStackTrace();
            break;
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    handle4capture.breakLoop();

    handle4capture.close();
    handle4send.close();
    executor.shutdown();
  }

}
