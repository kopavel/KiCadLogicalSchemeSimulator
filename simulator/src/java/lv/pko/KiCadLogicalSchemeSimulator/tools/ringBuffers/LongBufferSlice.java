package lv.pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
public class LongBufferSlice implements IRingBufferSlice {
    private final long[] slice;
    private int pos = -1;

    public LongBufferSlice(long[] slice) {
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
