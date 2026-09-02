/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.Switch;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.awt.Color.GRAY;
import static java.awt.Color.GREEN;

public class SwitchUiComponent extends AbstractUiComponent {
    private Color innerColor;

    public SwitchUiComponent(Switch parent, String title, boolean toggled) {
        super(title, 30);
        innerColor = toggled ? GREEN : GRAY;
        setBackground(new Color(0, 0, 0, 0));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Toggle the color of inner square on mouse click
                innerColor = (innerColor == GRAY) ? GREEN : GRAY;
                repaint(); // Repaint the panel to reflect the color change
                Thread.ofVirtual().name("Switch:" + getName()).start(() -> parent.toggle(innerColor == GREEN));
            }
        });
    }

    @Override
    public void draw(Graphics2D g2d) {
        // Draw outer square
        g2d.setColor(Color.BLACK);
        g2d.drawRect(2, titleHeight + 2, size - 4, getHeight() - titleHeight - 4);
        // Draw inner square with changing color
        g2d.setColor(innerColor);
        g2d.fillRect(4, titleHeight + 4, size - 8, getHeight() - titleHeight - 8);
    }
}
