/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.oscillator;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OscillatorUiComponent extends AbstractUiComponent {
    public final Oscillator parent;
    public final DecimalFormat formatter = new DecimalFormat("#,###");
    public volatile OscillatorUi ui;
    private long lastTicks;

    public OscillatorUiComponent(int size, String title, Oscillator parent) {
        super(title, size);
        this.parent = parent;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (ui == null) {
                    synchronized (parent) {
                        if (ui == null) {
                            ui = new OscillatorUi(OscillatorUiComponent.this);
                        }
                    }
                }
                ui.setVisible(true);
            }
        });
        setBackground(Color.white);
        //noinspection resource
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::repaint, 0, 1, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(this::getFreq, 0, 1, TimeUnit.SECONDS);
        parent.startIfDefault();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    protected void draw(Graphics2D g2d) {
        g2d.drawString(formatter.format((double) parent.currentFreq.getOpaque()), 0, titleHeight << 1);
    }

    private void getFreq() {
        double newFreq = (parent.ticks - lastTicks) / 2.0;
        double freq = parent.currentFreq.getOpaque();
        if (freq < newFreq * 0.9 || freq > newFreq * 1.1) {
            parent.currentFreq.setRelease(newFreq);
        } else {
            parent.currentFreq.setRelease((freq * 0.8) + ((parent.ticks - lastTicks) * 0.1));
        }
        lastTicks = parent.ticks;
    }
}

