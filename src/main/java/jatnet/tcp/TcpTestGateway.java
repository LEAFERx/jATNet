package jatnet.tcp;

import jatnet.athernet.Athernet;
import jatnet.athernet.AthernetAddress;
import jatnet.athernet.AthernetPacket;
import jatnet.mac.Mac;
import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.IpVersion;
import org.pcap4j.util.MacAddress;
import org.pcap4j.util.NifSelector;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpTestGateway {
  public static void main(String[] args) throws InterruptedException, PcapNativeException, NotOpenException, UnknownHostException, IllegalRawDataException {

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
    captureHandle.setFilter("tcp", BpfProgram.BpfCompileMode.OPTIMIZE);

    UnknownPacket.Builder payloadBuilder = new UnknownPacket.Builder();
    TcpPacket.Builder tcpBuilder = new TcpPacket.Builder();
    IpV4Packet.Builder ipv4Builder = new IpV4Packet.Builder();
    ipv4Builder
        .srcAddr(address)
        .version(IpVersion.IPV4)
        .tos(IpV4Rfc791Tos.newInstance((byte) 0))
        .ttl((byte) 100)
        .protocol(IpNumber.TCP)
        .identification((short) 100)
        .payloadBuilder(tcpBuilder)
        .correctChecksumAtBuild(true)
        .correctLengthAtBuild(true);
    EthernetPacket.Builder ethernetBuilder = new EthernetPacket.Builder();
    ethernetBuilder
        .srcAddr(MAC_ADDR)
        .dstAddr(GATEWAY_MAC_ADDR)
        .type(EtherType.IPV4)
        .payloadBuilder(ipv4Builder)
        .paddingAtBuild(true);

    final short natPort = 18976;

    int frameSize = 64;
    int gatewayMacAddr = 0;
    int nodeMacAddr = 1;
    AthernetAddress gatewayAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 1});
    AthernetAddress nodeAddr = new AthernetAddress(new byte[]{(byte) 192, (byte) 168, 1, 2});

    Mac mac = new Mac(gatewayMacAddr, frameSize, 400, 10);
    Athernet athernet = new Athernet(gatewayAddr, mac);

    final PacketListener listener = packet -> {
      if (packet.contains(TcpPacket.class)) {
        try {
          TcpPacket t = packet.get(TcpPacket.class);
          if (t.getHeader().getDstPort().value() == natPort) {
            System.out.println("got a tcp packet");
            athernet.send(nodeMacAddr, nodeAddr, t.getRawData());
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    };

    athernet.start();
    Thread.sleep(200);
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
    Thread.sleep(200);

    while (true) {
      AthernetPacket athernetPacket = athernet.receive();
      byte[] rawData = athernetPacket.getPayload();
      Inet4Address dstAddr = (Inet4Address) InetAddress.getByAddress(athernetPacket.getDstAddr().toBytes());
      System.out.println(Arrays.toString(rawData));
      TcpPacket tcpPacket = TcpPacket.newPacket(rawData, 0, rawData.length);
      TcpPacket.TcpHeader header = tcpPacket.getHeader();
      payloadBuilder.rawData(tcpPacket.getPayload().getRawData());
      tcpBuilder
          .payloadBuilder(payloadBuilder)
          .srcAddr(address)
          .dstAddr(dstAddr)
          .srcPort(header.getSrcPort())
          .dstPort(header.getDstPort())
          .sequenceNumber(header.getSequenceNumber())
          .acknowledgmentNumber(header.getAcknowledgmentNumber())
          .dataOffset(header.getDataOffset())
          .urg(header.getUrg())
          .ack(header.getAck())
          .psh(header.getPsh())
          .rst(header.getRst())
          .syn(header.getSyn())
          .fin(header.getFin())
          .window(header.getWindow())
          .urgentPointer(header.getUrgentPointer())
          .options(header.getOptions())
          .correctChecksumAtBuild(true)
          .correctLengthAtBuild(true);
      ipv4Builder.dstAddr(dstAddr);
      EthernetPacket packet = ethernetBuilder.build();
      sendHandle.sendPacket(packet);
    }
  }
}
