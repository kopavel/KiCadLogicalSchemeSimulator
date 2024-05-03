package lv.pko.DigitalNetSimulator.tools.ringBuffers;
public class ByteBufferSlice implements IRingBufferSlice {
    private final byte[] slice;
    private int pos;

    public ByteBufferSlice(byte[] slice) {
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
