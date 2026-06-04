package pko.KiCadLogicalSchemeSimulator.components.display;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;

public class ClockIn extends InPin {
    public byte[] ram = new byte[(10 << 12)];//10 * 4096
    public int ramOffset;
    public byte pixel;

    protected ClockIn(Display parent) {
        super("Clock", parent);
    }

    @Override
    public void setHi() {
        state = true;
    }

    @Override
    public void setLo() {
        state = false;
        ram[ramOffset++] = pixel;
    }
}
