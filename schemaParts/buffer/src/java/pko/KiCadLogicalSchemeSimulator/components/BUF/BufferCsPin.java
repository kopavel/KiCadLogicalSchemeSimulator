package pko.KiCadLogicalSchemeSimulator.components.BUF;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.wire.InPin;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class BufferCsPin extends InPin {
    public Bus qBus;
    public Bus dBus;
    public Buffer parent;

    public BufferCsPin(String id, Buffer parent, Bus dBus) {
        super(id, parent);
        this.parent = parent;
        this.qBus = parent.getOutBus("Q");
        this.dBus = dBus;
    }

    /*Optimiser constructor*/
    public BufferCsPin(BufferCsPin oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        qBus = oldPin.qBus;
        dBus = oldPin.dBus;
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
            int state;
            if ((bus = qBus).state != (state = dBus.state) || bus.hiImpedance) {
                bus.setState(state);
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public void setLo() {
        state = false;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            Bus bus;
            if ((bus = qBus).state != dBus.state || bus.hiImpedance) {
                bus.setState(dBus.state);
            }
            /*Optimiser line o blockEnd r block nr*/
        } else {
            if (!(qBus).hiImpedance) {
                qBus.setHiImpedance();
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public InPin getOptimised(ModelItem<?> source) {
        ClassOptimiser<BufferCsPin> optimiser = new ClassOptimiser<>(this).cut("o");
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        BufferCsPin build = optimiser.build();
        build.source = source;
        parent.dBus.csPin = build;
        parent.csPin = build;
        parent.replaceIn(this, build);
        return build;
    }
}
