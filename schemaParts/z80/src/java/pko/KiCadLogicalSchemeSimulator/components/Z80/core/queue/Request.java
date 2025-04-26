package pko.KiCadLogicalSchemeSimulator.components.Z80.core.queue;
public class Request {
    public int address = -1;
    public boolean read;
    public int payload;
    public Callback callback;
    public Request next;
    public boolean memory;
}
