/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.components.display;
import pko.KiCadLogicalSchemeSimulator.api.schemaPart.AbstractUiComponent;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class DisplayUiComponent extends AbstractUiComponent {
    private final Display parent;
    int hScaled;
    int vScaled;
    private BufferedImage image;
    private byte[] imageData;
    private int length;
    private byte[] ram;

    public DisplayUiComponent(String title, int size, int scaleFactor, Display parent) {
        super(title, size);
        this.scaleFactor = scaleFactor;
        this.parent = parent;
    }

    @Override
    protected void draw(Graphics2D g2d) {
        if (parent.vSize > 0) {
            int vSize = parent.vSize;
            int hSize = parent.hSize;
            if (image == null || image.getWidth() != hSize || image.getHeight() != vSize) {
                hScaled = hSize * scaleFactor;
                vScaled = vSize * scaleFactor;
                image = new BufferedImage(hSize, vSize, BufferedImage.TYPE_BYTE_GRAY);
                imageData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
                length = vSize * hSize;
                ram = parent.clock.ram;
            }
            System.arraycopy(ram, 0, imageData, 0, length);
            g2d.drawImage(image, 0, titleHeight + 5, hScaled, vScaled, this);
        }
    }
}
