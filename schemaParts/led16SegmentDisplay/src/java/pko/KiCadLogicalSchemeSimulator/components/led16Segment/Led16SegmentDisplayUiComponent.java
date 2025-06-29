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
    public int state;
    private final int[] segmentsTime = new int[8];
    private final int inertia;
    private Shape[] segmentsUi;
    private Led16SegmentDisplay parent;

    public Led16SegmentDisplayUiComponent(Led16SegmentDisplay parent, int size, Color on, Color off, String title) {
        super(title, size);
        this.parent = parent;
        if (parent.params.containsKey("inertia")) {
            inertia = Integer.parseInt(parent.params.get("inertia"));
        } else {
            inertia = 1;
        }
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
        if (segmentsUi == null) {
            segmentsUi = new Line2D[17];
            int y = titleHeight + 5;
            // Top segments
            segmentsUi[0] = new Line2D.Float(segmentWidth, y, halfWidth - segmentWidth, y);
            segmentsUi[1] = new Line2D.Float(halfWidth + segmentWidth, y, width - segmentWidth, y);
            // Middle segments
            segmentsUi[2] = new Line2D.Float(segmentWidth, y + halfHeight, halfWidth - segmentWidth, y + halfHeight);
            segmentsUi[3] = new Line2D.Float(halfWidth + segmentWidth, y + halfHeight, width - segmentWidth, y + halfHeight);
            // Bottom segments
            segmentsUi[4] = new Line2D.Float(segmentWidth, y + height, halfWidth - segmentWidth, y + height);
            segmentsUi[5] = new Line2D.Float(halfWidth + segmentWidth, y + height, width - segmentWidth, y + height);
            // Vertical segments
            segmentsUi[6] = new Line2D.Float(1, y + segmentHeight, 1, y + halfHeight - segmentHeight);
            segmentsUi[7] = new Line2D.Float(halfWidth, y + segmentHeight, halfWidth, y + halfHeight - segmentHeight);
            segmentsUi[8] = new Line2D.Float(width, y + segmentHeight, width, y + halfHeight - segmentHeight);
            segmentsUi[9] = new Line2D.Float(1, y + halfHeight + segmentHeight, 1, y + height - segmentHeight);
            segmentsUi[10] = new Line2D.Float(halfWidth, y + halfHeight + segmentHeight, halfWidth, y + height - segmentHeight);
            segmentsUi[11] = new Line2D.Float(width, y + halfHeight + segmentHeight, width, y + height - segmentHeight);
            // Diagonal segments
            segmentsUi[12] = new Line2D.Float(segmentWidth + 2, y + segmentHeight + 1, halfWidth - segmentWidth, y + halfHeight - segmentHeight - 1);
            segmentsUi[13] = new Line2D.Float(halfWidth + segmentWidth + 1, y + halfHeight - segmentHeight - 1, width - segmentWidth - 1, y + segmentHeight + 1);
            segmentsUi[14] = new Line2D.Float(segmentWidth + 2, y + height - segmentHeight - 1, halfWidth - segmentWidth, y + halfHeight + segmentHeight + 1);
            segmentsUi[15] =
                    new Line2D.Float(halfWidth + segmentWidth + 1, y + halfHeight + segmentHeight + 1, width - segmentWidth - 1, y + height - segmentHeight - 1);
            // Decimal point
            segmentsUi[16] = new Line2D.Float(width + 1.3f * segmentWidth, y + height, width + 1.3f * segmentWidth, y + height);
        }
        g2d.setStroke(new BasicStroke(3)); // Sets line thickness to 5
        for (int i = 0; i < 17; i++) {
            int bit = 1 << i;
            if ((parent.segmentsOn & bit) > 0) {
                if ((parent.segmentsOff & bit) == 0) {
                    segmentsTime[i] = inertia;
                } else if (segmentsTime[i] == 0) {
                    parent.segmentsOn &= ~bit;
                } else {
                    segmentsTime[i]--;
                }
            }
            g2d.setColor((segmentsTime[i] > 0) ? on : off);
            g2d.draw(segmentsUi[i]);
        }
    }
}
