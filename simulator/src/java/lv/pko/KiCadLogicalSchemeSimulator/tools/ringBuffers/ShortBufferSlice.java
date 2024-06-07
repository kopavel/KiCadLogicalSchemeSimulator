package lv.pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
public class ShortBufferSlice implements IRingBufferSlice {
    private final short[] slice;
    private int pos = -1;

    public ShortBufferSlice(short[] slice) {
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
