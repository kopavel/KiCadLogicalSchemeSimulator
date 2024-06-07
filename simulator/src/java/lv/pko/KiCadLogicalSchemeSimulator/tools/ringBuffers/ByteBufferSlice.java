package lv.pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
public class ByteBufferSlice implements IRingBufferSlice {
    private final byte[] slice;
    private int pos = -1;

    public ByteBufferSlice(byte[] slice) {
        this.slice = slice;
    }

    @Override
    public int size() {
        return slice.length;
    }

    @Override
    public long next() {
        return slice[++pos];
    }

    @Override
    public void skip() {
        pos++;
    }

    @Override
    public long peek() {
        return slice[pos];
    }
}
