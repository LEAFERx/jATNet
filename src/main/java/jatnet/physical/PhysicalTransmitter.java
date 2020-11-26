package jatnet.physical;

import com.github.snksoft.crc.CRC;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class PhysicalTransmitter implements Runnable {
  private final LinkedBlockingQueue<float[]> audioBuffer;
  private final LinkedBlockingQueue<byte[]> dataBuffer;

  private final int bitWidth;
  private final int asioBufferSize;

  public PhysicalTransmitter(
      LinkedBlockingQueue<float[]> audioBuffer,
      LinkedBlockingQueue<byte[]> dataBuffer,
      int bitWidth,
      int asioBufferSize
  ) {
    this.audioBuffer = audioBuffer;
    this.dataBuffer = dataBuffer;
    this.bitWidth = bitWidth;
    this.asioBufferSize = asioBufferSize;
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      if (asioBufferSize == 0) {
        continue;
      }

      try {
        byte[] data = dataBuffer.take();
        long crc = CRC.calculateCRC(CRC.Parameters.CRC16, data);
        byte[] frame = Arrays.copyOf(data, data.length + 2);
        frame[frame.length - 2] = (byte) ((crc >> 8) & 0xFF);
        frame[frame.length - 1] = (byte) (crc & 0xFF);
        float[] modulated = Modem.modulate(frame, bitWidth);
        int chunkNums = (int) Math.ceil((double) modulated.length / asioBufferSize);
        for (int i = 0; i < chunkNums - 1; i++) {
          float[] chunk = Arrays.copyOfRange(modulated, i * asioBufferSize, (i + 1) * asioBufferSize);
          audioBuffer.put(chunk);
        }
        float[] chunk = new float[asioBufferSize];
        for (int i = (chunkNums - 1) * asioBufferSize; i < chunkNums * asioBufferSize; i++) {
          int chunkIndex = i - (chunkNums - 1) * asioBufferSize;
          if (i < modulated.length) {
            chunk[chunkIndex] = modulated[i];
          } else {
            chunk[chunkIndex] = 0;
          }
        }
        audioBuffer.put(chunk);
        float[] paddingChunk = new float[asioBufferSize];
        Arrays.fill(paddingChunk, 0);
        audioBuffer.put(paddingChunk);
      } catch (InterruptedException e) {
        return;
      }
    }
  }
}
