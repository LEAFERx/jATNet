package jatnet.physical;


import com.synthbot.jasiohost.AsioChannel;
import com.synthbot.jasiohost.AsioDriverListener;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioListener implements AsioDriverListener {
  private final LinkedBlockingQueue<float[]> rxBuffer;
  private final LinkedBlockingQueue<float[]> txBuffer;
  private int bufferSize;
  private float[] zeroOutput;

  public AudioListener(LinkedBlockingQueue<float[]> rxBuffer, LinkedBlockingQueue<float[]> txBuffer, int bufferSize) {
    this.rxBuffer = rxBuffer;
    this.txBuffer = txBuffer;
    this.bufferSize = bufferSize;
    this.zeroOutput = new float[bufferSize];
    Arrays.fill(zeroOutput, 0);
  }

  @Override
  public void sampleRateDidChange(double sampleRate) {
    System.out.println("** Change sample rate to " + sampleRate);
  }

  @Override
  public void resetRequest() {
    System.out.println("** Reset request");
  }

  @Override
  public void resyncRequest() {
    System.out.println("** Resync request");
  }

  @Override
  public void bufferSizeChanged(int bufferSize) {
    System.out.println("** Buffer size changed to " + bufferSize);
    this.bufferSize = bufferSize;
    this.zeroOutput = new float[bufferSize];
    Arrays.fill(zeroOutput, 0);
  }

  @Override
  public void latenciesChanged(int inputLatency, int outputLatency) {
    System.out.println("** Latency changed. Input " + inputLatency + "; Output " + outputLatency);
  }

  @Override
  public void bufferSwitch(long sampleTime, long samplePosition, Set<AsioChannel> channels) {
    for (AsioChannel channel : channels) {
      if (channel.isInput()) {
        float[] input = new float[bufferSize];
        channel.read(input);
        rxBuffer.offer(input);
      } else {
        float[] output = txBuffer.poll();
        if (output != null) {
          channel.write(output);
        } else {
          channel.write(zeroOutput);
        }
      }
    }
  }
}