package lv.pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
public class LongRingBuffer extends RingBuffer {
    private final long[] data;

    public LongRingBuffer() {
        this(0);
    }

    public LongRingBuffer(int capacity) {
        super((capacity < 1) ? DEFAULT_CAPACITY : capacity);
        this.data = new long[this.capacity];
    }

    @Override
    public void put(long element) {
        super.put(element);
        data[writePos] = element;
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
        long[] retVal = new long[amount];
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
        return new LongBufferSlice(retVal);
    }
}