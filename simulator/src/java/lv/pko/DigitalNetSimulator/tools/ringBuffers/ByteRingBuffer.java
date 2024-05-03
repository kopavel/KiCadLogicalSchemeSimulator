package lv.pko.DigitalNetSimulator.tools.ringBuffers;
public class ByteRingBuffer extends RingBuffer {
    private final byte[] data;

    public ByteRingBuffer() {
        this(0);
    }

    public ByteRingBuffer(int capacity) {
        super((capacity < 1) ? DEFAULT_CAPACITY : capacity);
        this.data = new byte[this.capacity];
    }

    @Override
    public void put(long element) {
        super.put(element);
        data[writePos] = (byte) element;
    }

    @Override
    public IRingBufferSlice take(int offset, int amount) {
        int snapWritePos = writePos;
        int available = isFull ? capacity : snapWritePos + 1;
        if (offset > available) {
            offset = available;
        }
        if (amount > offset) {
            amount = offset;
        }
        byte[] retVal = new byte[amount];
        offset--;
        if (offset <= snapWritePos) {
            System.arraycopy(data, snapWritePos - offset, retVal, 0, amount);
        } else {
            int tailLength = offset - snapWritePos;
            if (amount <= tailLength) {
                System.arraycopy(data, capacity - tailLength - 1, retVal, 0, amount);
            } else {
                System.arraycopy(data, capacity - tailLength - 1, retVal, 0, tailLength);
                System.arraycopy(data, 0, retVal, tailLength, amount - tailLength);
            }
        }
        return new ByteBufferSlice(retVal);
    }
}
