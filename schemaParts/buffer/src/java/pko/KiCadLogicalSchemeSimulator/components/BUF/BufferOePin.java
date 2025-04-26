package pko.KiCadLogicalSchemeSimulator.components.BUF;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class BufferOePin extends InPin {
    public final Buffer parent;
    public Bus qBus;

    public BufferOePin(String id, Buffer parent) {
        super(id, parent);
        this.parent = parent;
        this.qBus = parent.getOutBus("Q");
    }

    /*Optimiser constructor*/
    public BufferOePin(BufferOePin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        qBus = oldPin.qBus;
    }

    @Override
    public void setHi() {
        state = true;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            if (!qBus.hiImpedance) {
                qBus.setHiImpedance();
            }
            /*Optimiser line o blockEnd r block nr*/
        } else {
            Bus bus;
            int latch;
            if ((bus = qBus).state != (latch = parent.latch) || bus.hiImpedance) {
                bus.setState(latch);
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public void setLo() {
        state = false;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            int latch;
            Bus bus;
            if ((bus = qBus).state != (latch = parent.latch) || bus.hiImpedance) {
                bus.setState(latch);
            }
            /*Optimiser line o blockEnd r block nr*/
        } else {
            if (!qBus.hiImpedance) {
                qBus.setHiImpedance();
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<BufferOePin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        BufferOePin build = optimiser.build();
        build.source = source;
        parent.oePin = build;
        parent.wrPin.oePin = build;
        parent.replaceIn(this, build);
        return build;
    }
}
