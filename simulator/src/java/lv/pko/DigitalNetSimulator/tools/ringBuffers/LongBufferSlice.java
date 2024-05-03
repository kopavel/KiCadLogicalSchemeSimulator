package lv.pko.DigitalNetSimulator.tools.ringBuffers;
public class LongBufferSlice implements IRingBufferSlice {
    private final long[] slice;
    private int pos;

    public LongBufferSlice(long[] slice) {
        this.slice = slice;
    }

    @Override
    public int size() {
        return slice.length;
    }

    @Override
    public long next() {
        return slice[pos++];
    }
}
