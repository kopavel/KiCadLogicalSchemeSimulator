package pko.KiCadLogicalSchemeSimulator.net;
import lombok.AllArgsConstructor;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.Bus;

@AllArgsConstructor
public class ResendBus implements ResendItem {
    public final Bus item;
    public final int state;

    public ResendBus(Bus item) {
        this.item = item;
        state = item.state;
    }

    @Override
    public void resend() {
        if (!item.hiImpedance) {
            item.setState(state);
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
        return state;
    }
}
