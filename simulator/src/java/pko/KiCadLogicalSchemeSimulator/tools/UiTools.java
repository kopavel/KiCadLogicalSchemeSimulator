/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"unused", "StaticMethodOnlyUsedInOneClass"})
public enum UiTools {
    ;
    private static final BufferedImage font;
    private static final Map<Byte, BufferedImage> letters = new HashMap<>();
    static {
        try {
            font = ImageIO.read(Objects.requireNonNull(UiTools.class.getResourceAsStream("/font.bmp")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static Color getColor(String hColor) {
        int red = Integer.parseInt(hColor.substring(1, 3), 16);
        int green = Integer.parseInt(hColor.substring(3, 5), 16);
        int blue = Integer.parseInt(hColor.substring(5, 7), 16);
        return new Color(red, green, blue);
    }

    public static void print(int value, int x, int y, int size, Graphics2D dest) {
        size = Math.max(String.format("%x", value).length(), size);
        x += size * 5;
        while (size-- > 0) {
            x -= 5;
            int pos = value & 0xf;
            value = value >> 4;
            BufferedImage letter = letters.computeIfAbsent((byte) pos, aPos -> font.getSubimage(aPos * 5, 0, 4, 7));
            dest.drawImage(letter, x, y, null);
        }
    }

    public static ImageIcon loadBase64Image(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return new ImageIcon(ImageIO.read(new ByteArrayInputStream(bytes)));
        } catch (Exception e) {
            Log.error(UiTools.class, "Icon load error", e);
            return null;
        }
    }

    public static String refreshIconBase64() {
        return "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAQAAADZc7J/AAABOElEQVR42qWVMW7CQBBFXw8SWAj5IClp3cV94iNYok5KqriAIwAu3VE6l3DBTZAQShosT6QpkuCJvTj7ppz/n9arlYyDFV6sEN" +
                "+6+NbFty5syUgIGcQL0pqakgV388AJMdOQMxquiEkpuCA6R8KhCpSADVdU0XeKMVbxQ8RZFTkdRFRMjeImoadouq6zRKiYtBS3rBGEEixztX8yo49Ar7MmxPCk7gMuCs0lGN50scRFqrkMw14XMS5ize0wbO8UPGpujyHTRYqLZdcnJLoocHHQ3DOGkBrhQkAfMz4Qrsy7HpKwgZ6nNKFCeOdPFjRqj/jNqaWYUhHRQY4gnG8CYhRjOhlxRPQUawJQxCh6CVGFXmdBSowMVYzIaRAzAxSwoKQ2ilcGEZKQsfuur/gfvnV867jrnj/4L8gVvwNYaEhhAAAAAElFTkSuQmCC";
    }

    @SuppressWarnings("AbstractClassWithOnlyOneDirectInheritor")
    public abstract static class TextChangeListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            textChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            textChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            textChanged();
        }

        protected abstract void textChanged();
    }

    public static double[] getFontSize(Font font) {
        JLabel label = new JLabel();
        FontMetrics metrics = label.getFontMetrics(font);
        String chars = "0123456789ABCDEF";
        double maxWidth = 0;
        for (int i = 0; i < chars.length(); i++) {
            maxWidth = Math.max(maxWidth, metrics.charWidth(chars.charAt(i)));
        }
        return new double[]{maxWidth, metrics.getHeight()};
    }
}
