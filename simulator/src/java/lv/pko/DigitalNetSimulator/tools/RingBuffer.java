package lv.pko.DigitalNetSimulator.tools;
import java.util.Arrays;
import java.util.List;

public class RingBuffer<E> {
    private static final int DEFAULT_CAPACITY = 10000000;
    private final int capacity;
    private final E[] data;
    private boolean isFull;
    private int writePos;

    public RingBuffer() {
        this(0);
    }

    public RingBuffer(int capacity) {
        this.capacity = (capacity < 1) ? DEFAULT_CAPACITY : capacity;
        this.data = (E[]) new Object[this.capacity];
        this.writePos = -1;
    }

    public void put(E element) {
        if (++writePos == capacity) {
            writePos = 0;
            isFull = true;
        }
        data[writePos] = element;
    }

    public List<E> take(int offset, int amount) {
        int snapWritePos = writePos;
        int available = isFull ? capacity : snapWritePos + 1;
        if (offset > available) {
            offset = available;
        }
        if (amount > offset) {
            amount = offset;
        }
        E[] retVal = (E[]) new Object[amount];
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
        return Arrays.stream(retVal).toList();
    }

    public int available() {
        return isFull ? capacity : writePos + 1;
    }
}
