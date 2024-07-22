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
package pko.KiCadLogicalSchemeSimulator.components.Switch;
import pko.KiCadLogicalSchemeSimulator.api.AbstractUiComponent;

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
                parent.toggle(innerColor == GREEN);
            }
        });
    }

    @Override
    public void draw() {
        // Draw outer square
        g2d.setColor(Color.BLACK);
        g2d.drawRect(2, titleHeight + 2, size - 4, getHeight() - titleHeight - 4);
        // Draw inner square with changing color
        g2d.setColor(innerColor);
        g2d.fillRect(4, titleHeight + 4, size - 8, getHeight() - titleHeight - 8);
    }
}
