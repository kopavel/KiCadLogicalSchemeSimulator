package pko.KiCadLogicalSchemeSimulator.components.display;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;

public class ClockIn extends InPin {
    private final Display display;
    public byte pixel;

    protected ClockIn(Display parent) {
        super("Clock", parent);
        display = parent;
    }

    @Override
    public void setHi() {
        state = true;
    }

    @Override
    public void setLo() {
        state = false;
        Display ldisplay = display;
        ldisplay.row[ldisplay.hPos++] = pixel;
    }
}
