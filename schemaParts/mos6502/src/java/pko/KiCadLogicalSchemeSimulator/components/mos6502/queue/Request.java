package pko.KiCadLogicalSchemeSimulator.components.mos6502.queue;
public class Request {
    public int address = -1;
    public boolean read;
    public long payload;
    public Callback callback;
    public Request next;
}
