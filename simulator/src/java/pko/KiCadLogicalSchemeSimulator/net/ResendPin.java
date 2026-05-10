package pko.KiCadLogicalSchemeSimulator.net;
import lombok.AllArgsConstructor;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.wire.Pin;

@AllArgsConstructor
public class ResendPin implements ResendItem {
    public final Pin item;
    public final boolean state;

    public ResendPin(Pin item) {
        this.item = item;
        state = item.state;
    }

    @Override
    public void resend() {
        if (!(item.hiImpedance)) {
            if (state) {
                item.setHi();
            } else {
                item.setLo();
            }
        }
    }

    @Override
    public String getName() {
        return item.getName();
    }

    @Override
    public IModelItem<?> getItem() {
        return item;
    }

    @Override
    public int getState() {
        return state ? 1 : 0;
    }
}
