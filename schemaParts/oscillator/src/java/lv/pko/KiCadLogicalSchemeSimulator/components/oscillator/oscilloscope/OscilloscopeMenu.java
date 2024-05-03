package lv.pko.KiCadLogicalSchemeSimulator.components.oscillator.oscilloscope;
import lv.pko.KiCadLogicalSchemeSimulator.Simulator;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.in.InPin;
import lv.pko.KiCadLogicalSchemeSimulator.api.pins.out.OutPin;

import javax.swing.*;
import java.util.List;

public class OscilloscopeMenu extends JMenuBar {
    private final Oscilloscope oscilloscope;

    public OscilloscopeMenu(Oscilloscope parent) {
        oscilloscope = parent;
        JMenu outPinMenu = new JMenu(Oscilloscope.localization.getString("outPins"));
        add(outPinMenu);
        JMenu inPinMenu = new JMenu(Oscilloscope.localization.getString("InPins"));
        add(inPinMenu);
        List<OutPin> outPins = Simulator.model.schemaParts.values()
                .stream()
                .flatMap(p -> p.outMap.values()
                        .stream())
                .toList();
        for (OutPin pin : outPins) {
            JMenuItem outPinItem = new JMenuItem(pin.getName());
            outPinItem.addActionListener(e -> oscilloscope.addPin(pin, pin.getName()));
            outPinMenu.add(outPinItem);
        }
        List<InPin> inPins = Simulator.model.schemaParts.values()
                .stream()
                .flatMap(p -> p.inMap.values()
                        .stream())
                .toList();
        for (InPin pin : inPins) {
            JMenuItem inPinItem = new JMenuItem(pin.getName());
            inPinItem.addActionListener(e -> oscilloscope.addPin(pin.source, pin.getName()));
            inPinMenu.add(inPinItem);
        }
    }
}
