package jatnet.physical;

import com.github.snksoft.crc.CRC;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

enum PhysicalReceiverState {
  DETECT,
  DECODE;
}

public class PhysicalReceiver implements Runnable {
  private final LinkedBlockingQueue<float[]> audioBuffer;
  private final LinkedBlockingQueue<byte[]> dataBuffer;

  private final int bitWidth;
  private final int frameSize;

  public PhysicalReceiver(LinkedBlockingQueue<float[]> audioBuffer, LinkedBlockingQueue<byte[]> dataBuffer, int bitWidth, int frameSize) {
    this.audioBuffer = audioBuffer;
    this.dataBuffer = dataBuffer;
    this.bitWidth = bitWidth;
    this.frameSize = frameSize;
  }

  @Override
  public void run() {
    float[] preamble = PhysicalUtils.getPreamble();
    PhysicalReceiverState state = PhysicalReceiverState.DETECT;
    float power = 0f;
    int startIndex = 0;
    CircularFifoQueue<Float> syncQueue = new CircularFifoQueue<>(preamble.length);
    for (int i = 0; i < preamble.length; i++) {
      syncQueue.add(0f);
    }
    float syncPowerLocalMax = 0f;
    float[] decode = new float[bitWidth * frameSize * 8];
    int currentDecodeIndex = 0;
    int totalCount = 0;
    int dataIndex = 0;

    float[] previousData = null;

    float syncPowerThreshold = 0.1f;

    while (!Thread.currentThread().isInterrupted()) {
      try {
        float[] data = audioBuffer.take();
        if (previousData == null) {
          previousData = new float[data.length];
        }
        int lastCount = totalCount;
        totalCount += data.length;

        while (dataIndex < totalCount) {
          float currentSample = data[dataIndex - lastCount];
          power = power * 63 / 64 + currentSample * currentSample / 64;

          if (state == PhysicalReceiverState.DETECT) {
            syncQueue.add(currentSample);
            int i = 0;
            float syncPower = 0;
            for (float sample : syncQueue) {
              syncPower += sample * preamble[i++];
            }
            if (syncPower > power && syncPower > syncPowerLocalMax && syncPower > syncPowerThreshold) {
              syncPowerLocalMax = syncPower;
              startIndex = dataIndex + 1;
            } else if (dataIndex - startIndex + 1 > preamble.length / 2 && startIndex != 0) {
              syncPowerLocalMax = 0;
              syncQueue.clear();
              for (int j = 0; j < preamble.length; j++) {
                syncQueue.add(0f);
              }
              state = PhysicalReceiverState.DECODE;
              if (startIndex < lastCount) {
                int j = 0;
                for (; j < lastCount - startIndex; j++) {
                  decode[j] = previousData[previousData.length - lastCount + startIndex - j];
                }
                for (; j <= dataIndex - startIndex; j++) {
                  decode[j] = data[j - lastCount + startIndex];
                }
              } else {
                for (int j = startIndex; j <= dataIndex; j++) {
                  decode[j - startIndex] = data[j - lastCount];
                }
              }
              currentDecodeIndex = dataIndex - startIndex + 1;
            }
          } else {
            decode[currentDecodeIndex++] = currentSample;
            if (currentDecodeIndex == decode.length) {
              byte[] decoded = Modem.demodulate(decode, bitWidth);
              validateAndPutData(decoded);
              startIndex = 0;
              currentDecodeIndex = 0;
              state = PhysicalReceiverState.DETECT;
            }
          }
          dataIndex += 1;
        }
        previousData = data.clone();
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  private void validateAndPutData(byte[] decoded) throws InterruptedException {
    byte[] data = Arrays.copyOf(decoded, frameSize - 2);
    long crc = CRC.calculateCRC(CRC.Parameters.CRC16, data);
    if (((crc >> 8) & 0xFF) == (decoded[frameSize - 2] & 0xFF) && (crc & 0xFF) == (decoded[frameSize - 1] & 0xFF)) {
      dataBuffer.put(data);
    }
  }
}
