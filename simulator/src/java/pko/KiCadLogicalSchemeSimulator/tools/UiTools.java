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

@SuppressWarnings("unused")
public class UiTools {
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
            BufferedImage letter = letters.computeIfAbsent((byte) pos, c -> font.getSubimage(c * 5, 0, 4, 7));
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
}
