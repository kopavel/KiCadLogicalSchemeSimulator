/*
 * Copyright (c) 2024 Pavel Korzh
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package lv.pko.DigitalNetSimulator.ui.oscilloscope;
import lv.pko.DigitalNetSimulator.api.pins.out.OutPin;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WatchedItem extends JPanel {
    public final OutPin pin;
    private final List<Long> states = new ArrayList<>();

    public WatchedItem(OutPin pin) {
        this.pin = pin;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    public void tick() {
        states.add(pin.state);
        if (states.size() > Oscilloscope.historySize) {
            states.removeFirst();
        }
        add(new FixedWidthLabel(String.format("%" + (int) Math.ceil(pin.size / 4d) + "X", pin.state), 30));
        revalidate();
        repaint();
    }

    public void update() {
        //repaint(); // Repaint the panel to reflect changes
    }

    private static class FixedWidthLabel extends JPanel {
        public FixedWidthLabel(String text, int width) {
            setMaximumSize(new Dimension(30, 20));
            setPreferredSize(new Dimension(width, 20));
            setLayout(new BorderLayout());
            add(new JLabel(text), BorderLayout.CENTER);
        }
    }
}
