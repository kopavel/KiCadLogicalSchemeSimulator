package lv.pko.KiCadLogicalSchemeSimulator.tools.ringBuffers;
public interface IRingBufferSlice {
    int size();
    long next();
    void skip();
    long peek();
}
