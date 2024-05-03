package lv.pko.DigitalNetSimulator.tools.ringBuffers;
public class ShortRingBuffer extends RingBuffer {
    private final short[] data;

    public ShortRingBuffer() {
        this(0);
    }

    public ShortRingBuffer(int capacity) {
        super((capacity < 1) ? DEFAULT_CAPACITY : capacity);
        this.data = new short[this.capacity];
    }

    @Override
    public void put(long element) {
        super.put(element);
        data[writePos] = (short) element;
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
        short[] retVal = new short[amount];
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
        return new ShortBufferSlice(retVal);
    }
}
