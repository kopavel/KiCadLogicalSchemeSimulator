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
package pko.KiCadLogicalSchemeSimulator.components.display;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class DisplayUiComponent extends AbstractUiComponent {
    public final int scaleFactor;
    private final Display parent;

    public DisplayUiComponent(String title, int size, int scaleFactor, Display parent) {
        super(title, size);
        this.scaleFactor = scaleFactor;
        this.parent = parent;
    }

    @Override
    protected void draw() {
        if (parent.vSize > 0) {
            BufferedImage image = new BufferedImage(parent.hSize, parent.vSize, BufferedImage.TYPE_BYTE_GRAY);
            byte[][] snapshot = parent.ram;
            WritableRaster raster = image.getRaster();
            for (int x = 0; x < parent.hSize; x++) {
                for (int y = 0; y < parent.vSize; y++) {
                    byte pixelValue = snapshot[y][x];
                    raster.setSample(x, y, 0, pixelValue);
                }
            }
            AffineTransform at = AffineTransform.getScaleInstance(scaleFactor, scaleFactor);
            g2d.transform(at);
            g2d.drawImage(image, 0, titleHeight, this);
        }
    }
}
