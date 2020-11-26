package jatnet.physical;

import com.synthbot.jasiohost.AsioChannel;
import com.synthbot.jasiohost.AsioDriver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Physical {
  private final LinkedBlockingQueue<float[]> rxAudioBuffer = new LinkedBlockingQueue<>();
  private final LinkedBlockingQueue<float[]> txAudioBuffer = new LinkedBlockingQueue<>();

  private final LinkedBlockingQueue<byte[]> rxDataBuffer = new LinkedBlockingQueue<>();
  private final LinkedBlockingQueue<byte[]> txDataBuffer = new LinkedBlockingQueue<>();

  private final Thread rxThread;
  private final Thread txThread;

  private final AsioDriver asioDriver;

  private final int bitWidth;
  private final int frameSize;
  private boolean started = false;

  public Physical(int bitWidth, int frameSize) {
    List<String> driverNameList = AsioDriver.getDriverNames();
    asioDriver = AsioDriver.getDriver(driverNameList.get(0));

    this.bitWidth = bitWidth;
    this.frameSize = frameSize + 2;

    PhysicalReceiver receiver = new PhysicalReceiver(rxAudioBuffer, rxDataBuffer, bitWidth, this.frameSize);
    PhysicalTransmitter transmitter = new PhysicalTransmitter(txAudioBuffer, txDataBuffer, bitWidth, getBufferSize());

    rxThread = new Thread(receiver);
    txThread = new Thread(transmitter);
  }

  public int getBufferSize() {
    return asioDriver.getBufferPreferredSize();
  }

  public double getSampleRate() {
    return asioDriver.getSampleRate();
  }

  public int getBitWidth() {
    return bitWidth;
  }

  public int getFrameSize() {
    return frameSize;
  }

  public float senseChannel() {
    return 0f;
  }

  public void start() {
    if (started) {
      return;
    }

    if (!PhysicalUtils.load()) {
      System.out.println("Failed to load physical layer resources.");
      return;
    }

    double sampleRate = getSampleRate();
    int bufferSize = getBufferSize();
    AudioListener listener = new AudioListener(rxAudioBuffer, txAudioBuffer, bufferSize);
    asioDriver.addAsioDriverListener(listener);

    Set<AsioChannel> activeChannels = new HashSet<>();
    activeChannels.add(asioDriver.getChannelInput(0));
    activeChannels.add(asioDriver.getChannelOutput(0));
    asioDriver.createBuffers(activeChannels);

    rxThread.start();
    txThread.start();

    asioDriver.start();

    started = true;
  }

  public void stop() {
    if (!started) {
      return;
    }

    rxThread.interrupt();
    txThread.interrupt();
    try {
      rxThread.join();
      txThread.join();
    } catch (InterruptedException ignored) {
    }

    asioDriver.shutdownAndUnloadDriver();

    started = false;
  }

  public byte[] read() throws InterruptedException {
    return read(-1);
  }

  public byte[] readNoWait() throws InterruptedException {
    return read(0);
  }

  public byte[] read(long timeout) throws InterruptedException {
    if (timeout == -1) {
      return rxDataBuffer.take();
    } else if (timeout == 0) {
      return rxDataBuffer.poll();
    } else {
      return rxDataBuffer.poll(timeout, TimeUnit.MILLISECONDS);
    }
  }

  public void write(byte[] data) throws InterruptedException {
    txDataBuffer.put(data);
  }
}
