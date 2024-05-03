package lv.pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
public class IntBufferSlice implements IRingBufferSlice {
    private final int[] slice;
    private int pos;

    public IntBufferSlice(int[] slice) {
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
