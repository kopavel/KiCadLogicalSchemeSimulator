package pko.KiCadLogicalSchemeSimulator.components.mos6502.queue;
@FunctionalInterface
public interface Callback {
    void accept(int data);
}
