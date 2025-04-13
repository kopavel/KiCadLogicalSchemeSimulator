package pko.KiCadLogicalSchemeSimulator.components.Z80.core.queue;
@FunctionalInterface
public interface Callback {
    void accept(int data);
}
