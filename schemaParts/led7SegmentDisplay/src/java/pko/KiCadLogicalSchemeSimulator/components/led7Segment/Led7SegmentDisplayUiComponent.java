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
package pko.KiCadLogicalSchemeSimulator.components.led7Segment;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Led7SegmentDisplayUiComponent extends AbstractUiComponent {
    private final Color on;
    private final Color off;
    private final float segmentWidth; // Thickness of segments
    private final float segmentHeight; // Thickness of segments
    private final float halfHeight;
    private final float width;
    private final float height;
    private final Led7SegmentDisplay parent;
    private Shape[] segment;

    public Led7SegmentDisplayUiComponent(Led7SegmentDisplay parent, int size, Color on, Color off, String title) {
        super(title, size);
        this.parent = parent;
        this.on = on;
        this.off = off;
        setBackground(new Color(0, 0, 0, 0));
        width = 0.5f * size;
        height = size - 5;
        segmentWidth = 6.0f;
        segmentHeight = 6.0f;
        halfHeight = height / 2;
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
        if (segment == null) {
            segment = new Shape[8];
            int y = titleHeight + 5;
            // Top segments
            segment[0] = new Line2D.Float(segmentWidth, y, width - segmentWidth, y);
            // Middle segments
            segment[1] = new Line2D.Float(segmentWidth, y + halfHeight, width - segmentWidth, y + halfHeight);
            // Bottom segments
            segment[2] = new Line2D.Float(segmentWidth, y + height, width - segmentWidth, y + height);
            // Vertical segments
            segment[3] = new Line2D.Float(1, y + segmentHeight, 1, y + halfHeight - segmentHeight);
            segment[4] = new Line2D.Float(width, y + segmentHeight, width, y + halfHeight - segmentHeight);
            segment[5] = new Line2D.Float(1, y + halfHeight + segmentHeight, 1, y + height - segmentHeight);
            segment[6] = new Line2D.Float(width, y + halfHeight + segmentHeight, width, y + height - segmentHeight);
            // Decimal point
            segment[7] = new Rectangle2D.Float(width + 2 * segmentWidth, y + height - segmentHeight, segmentWidth, segmentHeight);
        }
        g2d.setStroke(new BasicStroke(5)); // Sets line thickness to 5
        for (int i = 0; i < 8; i++) {
            if (segment[i] != null) {
                int bit = 1 << i;
                g2d.setColor((parent.segments & bit) > 0 ? on : off);
                g2d.draw(segment[i]);
                g2d.fill(segment[i]);
            }
        }
    }
}
