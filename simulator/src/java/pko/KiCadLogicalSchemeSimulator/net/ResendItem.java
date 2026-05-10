package pko.KiCadLogicalSchemeSimulator.net;
import pko.KiCadLogicalSchemeSimulator.api.IModelItem;

public interface ResendItem {
    void resend();
    String getName();
    IModelItem<?> getItem();
    int getState();
}
