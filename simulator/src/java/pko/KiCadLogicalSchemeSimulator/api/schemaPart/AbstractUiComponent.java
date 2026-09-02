/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.api.schemaPart;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public abstract class AbstractUiComponent extends Component {
    public static final int redrawPeriod = 50;
    public static final Font arialFont = new Font("Arial", Font.BOLD, 14);
    public static final Font monospacedFont = new Font("Courier New", Font.PLAIN, 12);
    protected final int size;
    private final String title;
    public int scaleFactor = 1;
    public int currentX, currentY;
    public boolean hasStoredLayout;
    public boolean sized;
    protected int titleHeight;
    private int mouseX, mouseY;

    protected AbstractUiComponent(String title, int size) {
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
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2d.setFont(arialFont);
        if (!sized) {
            FontMetrics metrics = g2d.getFontMetrics(arialFont);
            int textWidth = Math.max(metrics.stringWidth(title), size);
            titleHeight = metrics.getHeight();
            setSize(textWidth, titleHeight + size + 5);
            sized = true;
        }
        g2d.drawString(title, 0, titleHeight);
        draw(g2d);
    }

    abstract protected void draw(Graphics2D g2d);
}

