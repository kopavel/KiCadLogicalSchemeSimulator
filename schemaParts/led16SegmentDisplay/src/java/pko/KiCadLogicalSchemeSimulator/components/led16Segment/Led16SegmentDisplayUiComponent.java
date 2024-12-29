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
package pko.KiCadLogicalSchemeSimulator.components.led16Segment;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Led16SegmentDisplayUiComponent extends AbstractUiComponent {
    private final Color on;
    private final Color off;
    private final float segmentWidth; // Thickness of segments
    private final float segmentHeight; // Thickness of segments
    private final float halfWidth;
    private final float halfHeight;
    private final float width;
    private final float height;
    public long state;
    private Line2D[] segment;

    public Led16SegmentDisplayUiComponent(int size, Color on, Color off, String title) {
        super(title, size);
        this.on = on;
        this.off = off;
        setBackground(new Color(0, 0, 0, 0));
        width = 0.5f * size;
        height = size - 5;
        segmentWidth = 3.5f;
        segmentHeight = 3.5f;
        halfWidth = width / 2;
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
            segment = new Line2D[17];
            int y = titleHeight + 5;
            // Top segments
            segment[0] = new Line2D.Float(segmentWidth, y, halfWidth - segmentWidth, y);
            segment[1] = new Line2D.Float(halfWidth + segmentWidth, y, width - segmentWidth, y);
            // Middle segments
            segment[2] = new Line2D.Float(segmentWidth, y + halfHeight, halfWidth - segmentWidth, y + halfHeight);
            segment[3] = new Line2D.Float(halfWidth + segmentWidth, y + halfHeight, width - segmentWidth, y + halfHeight);
            // Bottom segments
            segment[4] = new Line2D.Float(segmentWidth, y + height, halfWidth - segmentWidth, y + height);
            segment[5] = new Line2D.Float(halfWidth + segmentWidth, y + height, width - segmentWidth, y + height);
            // Vertical segments
            segment[6] = new Line2D.Float(1, y + segmentWidth, 1, y + halfHeight - segmentWidth);
            segment[7] = new Line2D.Float(halfWidth, y + segmentWidth, halfWidth, y + halfHeight - segmentWidth);
            segment[8] = new Line2D.Float(width, y + segmentWidth, width, y + halfHeight - segmentWidth);
            segment[9] = new Line2D.Float(1, y + halfHeight + segmentWidth, 1, y + height - segmentWidth);
            segment[10] = new Line2D.Float(halfWidth, y + halfHeight + segmentWidth, halfWidth, y + height - segmentWidth);
            segment[11] = new Line2D.Float(width, y + halfHeight + segmentWidth, width, y + height - segmentWidth);
            // Diagonal segments
            segment[12] = new Line2D.Float(segmentWidth + 2, y + segmentHeight + 1, halfWidth - segmentWidth, y + halfHeight - segmentHeight - 1);
            segment[13] = new Line2D.Float(halfWidth + segmentWidth + 1, y + halfHeight - segmentHeight - 1, width - segmentWidth - 1, y + segmentHeight + 1);
            segment[14] = new Line2D.Float(segmentWidth + 2, y + height - segmentHeight - 1, halfWidth - segmentWidth, y + halfHeight + segmentHeight + 1);
            segment[15] =
                    new Line2D.Float(halfWidth + segmentWidth + 1, y + halfHeight + segmentHeight + 1, width - segmentWidth - 1, y + height - segmentHeight - 1);
            // Decimal point
            segment[16] = new Line2D.Float(width + 1.3f * segmentWidth, y + height, width + 1.3f * segmentWidth, y + height);
        }
        g2d.setStroke(new BasicStroke(3)); // Sets line thickness to 5
        for (int i = 0; i < 17; i++) {
            if (segment[i] != null) {
                int bit = 1 << i;
                g2d.setColor((state & bit) > 0 ? on : off);
                g2d.draw(segment[i]);
            }
        }
    }
}
