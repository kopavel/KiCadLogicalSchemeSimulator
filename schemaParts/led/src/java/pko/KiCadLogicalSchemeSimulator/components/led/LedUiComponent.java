/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.led;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LedUiComponent extends AbstractUiComponent {
    private final Color on;
    private final Color off;
    private final StateProvider provider;
    private Ellipse2D circle;

    public LedUiComponent(int size, Color on, Color off, String title, StateProvider provider) {
        super(title, size);
        this.on = on;
        this.off = off;
        this.provider = provider;
        setBackground(new Color(0, 0, 0, 0));
        //noinspection resource
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::repaint, 0, redrawPeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    protected void draw(Graphics2D g2d) {
        // Set color and draw the circle
        if (circle == null) {
            circle = new Ellipse2D.Float((float) (getWidth() - size) / 2, getHeight() - size - 2, size, size);
        }
        g2d.setColor(provider.isActive() ? on : off);
        g2d.fill(circle);
        // Draw the circle border
        g2d.setColor(Color.black);
        g2d.draw(circle);
    }
    @FunctionalInterface
    public interface StateProvider{
        boolean isActive();
    }
}
