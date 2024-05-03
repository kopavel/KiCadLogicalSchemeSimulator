package lv.pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
public abstract class RingBuffer {
    protected static final int DEFAULT_CAPACITY = 10000000;
    protected final int capacity;
    protected boolean isFull;
    protected int writePos;

    public RingBuffer(int capacity) {
        this.capacity = capacity;
        this.writePos = -1;
    }

    public int available() {
        return isFull ? capacity : writePos + 1;
    }

    public void put(long element) {
        if (++writePos == capacity) {
            writePos = 0;
            isFull = true;
        }
    }

    public abstract IRingBufferSlice take(int offset, int amount);
}
