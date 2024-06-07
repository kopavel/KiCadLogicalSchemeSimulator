package lv.pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
public class IntBufferSlice implements IRingBufferSlice {
    private final int[] slice;
    private int pos = -1;

    public IntBufferSlice(int[] slice) {
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
