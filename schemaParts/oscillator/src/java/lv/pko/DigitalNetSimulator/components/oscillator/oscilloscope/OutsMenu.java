package lv.pko.DigitalNetSimulator.components.oscillator.oscilloscope;
import lv.pko.DigitalNetSimulator.Simulator;
import lv.pko.DigitalNetSimulator.api.pins.out.OutPin;

import javax.swing.*;
import java.util.List;

public class OutsMenu extends JMenuBar {
    private final Oscilloscope oscilloscope;

    public OutsMenu(Oscilloscope parent) {
        oscilloscope = parent;
        JMenu outPins = new JMenu(Oscilloscope.localization.getString("outPins"));
        add(outPins);
        List<OutPin> pins = Simulator.model.schemaParts.values()
                .stream()
                .flatMap(p -> p.outMap.values()
                        .stream())
                .toList();
        for (OutPin pin : pins) {
            JMenuItem OutPinItem = new JMenuItem(pin.getName());
            OutPinItem.addActionListener(e -> oscilloscope.addPin(pin));
            outPins.add(OutPinItem);
        }
    }
}
