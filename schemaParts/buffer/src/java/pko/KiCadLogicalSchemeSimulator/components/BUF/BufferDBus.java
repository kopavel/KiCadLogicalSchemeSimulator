package pko.KiCadLogicalSchemeSimulator.components.BUF;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;
import pko.KiCadLogicalSchemeSimulator.api.bus.InBus;
import pko.KiCadLogicalSchemeSimulator.optimiser.ClassOptimiser;

public class BufferDBus extends InBus {
    private final Buffer parent;
    public BufferCsPin csPin;
    public Bus qBus;

    public BufferDBus(String id, Buffer parent, int size, String... names) {
        super(id, parent, size, names);
        this.qBus = parent.getOutBus("Q");
        this.parent = parent;
    }

    /*Optimiser constructor*/
    public BufferDBus(BufferDBus oldPin, String variantId) {
        super(oldPin, variantId);
        parent = oldPin.parent;
        qBus = oldPin.qBus;
        csPin = oldPin.csPin;
    }

    @Override
    public void setState(int newState) {
        state = newState;
        Bus bus;
        /*Optimiser line o block r*/
        if (parent.reverse) {
            if (!csPin.state && ((bus = qBus).state != newState || bus.hiImpedance)) {
                bus.setState(newState);
            }
            /*Optimiser line o blockEnd r block nr*/
        } else {
            if (csPin.state && ((bus = qBus).state != newState || bus.hiImpedance)) {
                bus.setState(newState);
            }
            /*Optimiser line o blockEnd nr*/
        }
    }

    @Override
    public InBus getOptimised(ModelItem<?> source) {
        ClassOptimiser<BufferDBus> optimiser = new ClassOptimiser<>(this).cut("o");
        if (parent.reverse) {
            optimiser.cut("nr");
        } else {
            optimiser.cut("r");
        }
        BufferDBus build = optimiser.build();
        build.source = source;
        parent.dBus = build;
        parent.csPin.dBus = build;
        parent.replaceIn(this, build);
        return build;
    }
}
