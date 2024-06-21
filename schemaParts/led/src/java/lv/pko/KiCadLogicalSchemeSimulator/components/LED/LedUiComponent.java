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
package lv.pko.KiCadLogicalSchemeSimulator.components.LED;
import lombok.Setter;
import lv.pko.KiCadLogicalSchemeSimulator.api.AbstractUiComponent;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LedUiComponent extends AbstractUiComponent {
    private final Color on;
    private final Color off;
    private Ellipse2D circle;
    @Setter
    private boolean state;

    public LedUiComponent(int size, Color on, Color off, String title) {
        super(title, size);
        this.on = on;
        this.off = off;
        setBackground(new Color(0, 0, 0, 0));
        //noinspection resource
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::repaint, 0, redrawPeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    protected void draw() {
        // Set color and draw the circle
        if (circle == null) {
            circle = new Ellipse2D.Double((double) (getWidth() - size) / 2, getHeight() - size - 2, size, size);
        }
        g2d.setColor(state ? on : off);
        g2d.fill(circle);
        // Draw the circle's border
        g2d.setColor(Color.black);
        g2d.draw(circle);
    }
}
