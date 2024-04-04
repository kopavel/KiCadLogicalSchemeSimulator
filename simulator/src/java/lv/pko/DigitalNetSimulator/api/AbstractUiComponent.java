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
package lv.pko.DigitalNetSimulator.api;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public abstract class AbstractUiComponent extends Component {
    public static final int redrawPeriod = 200;
    public static final Font arialFont = new Font("Arial", Font.BOLD, 14);
    public static final Font monospacedFont = new Font("Courier New", Font.PLAIN, 12);
    protected final int size;
    private final String title;
    public int currentX, currentY;
    public boolean hasStoredLayout;
    protected int titleHeight;
    protected Graphics2D g2d;
    protected Graphics g;
    protected boolean sized;
    private int mouseX, mouseY;

    public AbstractUiComponent(String title, int size) {
        this.title = title;
        this.size = size;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @SuppressWarnings("deprecation")
            @Override
            public void mouseDragged(MouseEvent e) {
                currentX = getX() + e.getX() - mouseX;
                currentY = getY() + e.getY() - mouseY;
                reshape(currentX, currentY, getWidth(), getHeight());
            }
        });
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        if (!hasStoredLayout) {
            super.setBounds(x, y, width, height);
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        this.g = g;
        g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(arialFont);
        if (!sized) {
            FontMetrics metrics = g2d.getFontMetrics(arialFont);
            int textWidth = Math.max(metrics.stringWidth(title), size);
            titleHeight = metrics.getHeight();
            setSize(textWidth, titleHeight + size + 5);
            sized = true;
        }
        g2d.drawString(title, 0, titleHeight);
        draw();
        g2d.dispose();
    }

    abstract protected void draw();
}

