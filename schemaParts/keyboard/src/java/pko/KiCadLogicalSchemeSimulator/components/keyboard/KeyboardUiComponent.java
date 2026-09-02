/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.keyboard;
import pko.KiCadLogicalSchemeSimulator.Simulator;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;

public class KeyboardUiComponent extends AbstractUiComponent implements KeyListener {
    public final Keyboard parent;
    private final Collection<String> keys = new ArrayList<>();
    private String label;

    public KeyboardUiComponent(String title, int size, Keyboard parent) {
        super(title, size);
        this.parent = parent;
        label = "No key pressed";
        Simulator.ui.addKeyListener(this);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        String keyText = KeyEvent.getKeyText(e.getKeyCode());
        if (!keys.contains(keyText)) {
            keys.add(keyText);
            label = String.join(" ", keys);
            parent.keyEvent(keyText, true);
            repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        String keyText = KeyEvent.getKeyText(e.getKeyCode());
        keys.remove(keyText);
        if (keys.isEmpty()) {
            label = "No key pressed";
        } else {
            label = String.join(" ", keys);
        }
        parent.keyEvent(keyText, false);
        repaint();
    }

    @Override
    protected void draw(Graphics2D g2d) {
        g2d.drawString(label, 0, titleHeight << 1);
    }
}
