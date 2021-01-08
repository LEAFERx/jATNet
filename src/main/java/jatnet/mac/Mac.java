package jatnet.mac;

import jatnet.physical.Physical;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Mac implements Runnable {
  private final Physical phy;

  private final LinkedBlockingQueue<MacFrame> rxFrameBuffer = new LinkedBlockingQueue<>();
  private final LinkedBlockingQueue<MacFrame> txFrameBuffer = new LinkedBlockingQueue<>();
  private final LinkedBlockingQueue<Boolean> txResultBuffer = new LinkedBlockingQueue<>();

  private final Thread macThread;

  private final int addr;
  private final int frameSize;
  private final long ACKTimeout;
  private final int maxRetransmitTimes;

  private final byte[] ACKData;

  private boolean started = false;

  public Mac(int addr, int frameSize, long ACKTimeout, int maxRetransmitTimes) {
    this.addr = addr;
    this.frameSize = frameSize + 2;
    this.ACKTimeout = ACKTimeout;
    this.maxRetransmitTimes = maxRetransmitTimes;

    phy = new Physical(6, this.frameSize);
    macThread = new Thread(this);

    ACKData = new byte[frameSize];
    for (int i = 0; i < frameSize; i++) {
      if (i % 3 == 0) {
        ACKData[i] = 'A';
      } else if (i % 3 == 1) {
        ACKData[i] = 'C';
      } else {
        ACKData[i] = 'K';
      }
    }
  }

  public Mac(int addr) {
    this(addr, 25, 200, 5);
  }

  public void start() {
    if (started) {
      return;
    }

    macThread.start();
    phy.start();

    started = true;
  }

  public void stop() {
    if (!started) {
      return;
    }

    macThread.interrupt();
    try {
      macThread.join();
    } catch (InterruptedException ignored) {
    }

    phy.stop();

    started = false;
  }

  public MacFrame read() throws InterruptedException {
    return read(-1);
  }

  public MacFrame readNoWait() throws InterruptedException {
    return read(0);
  }

  public MacFrame read(long timeout) throws InterruptedException {
    if (timeout == -1) {
      return rxFrameBuffer.take();
    } else if (timeout == 0) {
      return rxFrameBuffer.poll();
    } else {
      return rxFrameBuffer.poll(timeout, TimeUnit.MILLISECONDS);
    }
  }

  public boolean write(MacFrame frame) throws InterruptedException {
    txFrameBuffer.put(frame);
    return txResultBuffer.take();
  }

  public boolean write(int dest, byte[] data) throws InterruptedException {
    MacFrame frame = new MacFrame(dest, addr, MacFrameType.DATA, MacFrame.REQUIRE_ACK, data);
    txFrameBuffer.put(frame);
    return txResultBuffer.take();
  }

  public boolean write(int dest, byte[] data, int flags) throws InterruptedException {
    MacFrame frame = new MacFrame(dest, addr, MacFrameType.DATA, flags, data);
    txFrameBuffer.put(frame);
    return txResultBuffer.take();
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        byte[] phyData = phy.readNoWait();
        if (phyData != null) {
          if (phyData.length == frameSize) {
            MacFrame frame = MacFrame.parseFrame(phyData);
            if (frame.dest == addr) {
              rxFrameBuffer.put(frame);
              if (frame.requireACK()) {
                sendACK(frame.src);
              }
            }
          }
        } else {
          MacFrame txFrame = txFrameBuffer.poll();
          if (txFrame != null) {
            // CSMA here
            byte[] txData = txFrame.toBytes();
            phy.write(txData);
            if (txFrame.requireACK()) {
              boolean success = false;
              for (int i = 0; i < maxRetransmitTimes; i++) {
                byte[] data = phy.read(ACKTimeout);
                if (data != null) {
                  MacFrame frame = MacFrame.parseFrame(data);
                  if (frame.dest == addr) {
                    if (frame.type == MacFrameType.ACK) {
                      success = true;
                      break;
                    }
                  }
                }
                if (i != maxRetransmitTimes - 1) {
                  // Retransmit
                  phy.write(txData);
                }
              }
              txResultBuffer.put(success);
            } else {
              txResultBuffer.put(true);
            }
          }
        }
      } catch (InterruptedException e) {
        return;
      }
    }
  }

  private void sendACK(int dest) throws InterruptedException {
    phy.write(new MacFrame(dest, addr, MacFrameType.ACK, 0, ACKData).toBytes());
  }

  public int getFramePayloadSize() {
    return frameSize - 2;
  }
}
