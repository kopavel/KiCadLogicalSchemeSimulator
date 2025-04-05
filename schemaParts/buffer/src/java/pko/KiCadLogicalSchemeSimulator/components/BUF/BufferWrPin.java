package pko.KiCadLogicalSchemeSimulator.components.BUF;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class BufferWrPin extends InPin {
    public final Buffer parent;
    public Bus dBus;
    public BufferOePin oePin;
    public Bus qBus;

    public BufferWrPin(String id, Buffer parent, Bus dBus, BufferOePin oePin, Bus qBus) {
        super(id, parent);
        this.parent = parent;
        this.dBus = dBus;
        this.oePin = oePin;
        this.qBus = qBus;
    }

    /*Optimiser constructor*/
    public BufferWrPin(BufferWrPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        qBus = oldPin.qBus;
        dBus = oldPin.dBus;
        oePin = oldPin.oePin;
    }

    @Override
    public void setHi() {
        /*Optimiser line setter*/
        state = true;
        /*Optimiser line o block nr*/
        if (!parent.reverse) {
            long state;
            Bus bus;
            parent.latch = (state = dBus.state);
            if (oePin.state && ((bus = qBus).state != state || bus.hiImpedance)) {
                bus.setState(state);
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public void setLo() {
        /*Optimiser line setter*/
        state = false;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            long state;
            Bus bus;
            parent.latch = (state = dBus.state);
            if (!oePin.state && ((bus = qBus).state != state || bus.hiImpedance)) {
                bus.setState(state);
            }
            /*Optimiser line o blockEnd r*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<BufferWrPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        BufferWrPin build = optimiser.build();
        if (source != null) {
            optimiser.cut("setter");
        }
        build.source = source;
        parent.wrPin = build;
        parent.replaceIn(this, build);
        return build;
    }
}
