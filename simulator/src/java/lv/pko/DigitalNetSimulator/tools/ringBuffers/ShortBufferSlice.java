package lv.pko.DigitalNetSimulator.tools.ringBuffers;
public class ShortBufferSlice implements IRingBufferSlice {
    private final short[] slice;
    private int pos;

    public ShortBufferSlice(short[] slice) {
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
