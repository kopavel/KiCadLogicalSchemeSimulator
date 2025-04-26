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
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class Utils {
    private Utils() {
    }

    public static String getStackTrace() {
        return getStackTrace(3);
    }

    public static String getStackTrace(int trim) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        return Arrays.stream(st).skip(trim)
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n\t"));
    }

    public static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public static String readFileToString(File file) throws IOException {
        if (file.exists()) {
            FileInputStream is = new FileInputStream(file);
            return readStreamToString(is);
        }
        return "";
    }

    public static String readStreamToString(InputStream stream) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!content.toString().isEmpty()) {
                content.append(System.lineSeparator());
            }
            content.append(line);
        }
        return content.toString();
    }

    public static <T> T[] addToArray(final T[] array, final T item) {
        final int length = array.length;
        final T[] newArray = Arrays.copyOf(array, length + 1);
        newArray[length] = item;
        return newArray;
    }

    public static int getMaskForSize(int size) {
        int retVal = 0;
        for (int i = 0; i < size; i++) {
            retVal = (retVal << 1) | 1;
        }
        return retVal;
    }

    @SafeVarargs
    public static String getHash(Collection<? extends ModelItem<?>>... items) {
        Stream<String> mergedStream = null;
        for (Collection<? extends ModelItem<?>> item : items) {
            Stream<String> itemStream = item.stream()
                    .map(modelItem -> {
                        String result = modelItem.getName();
                        if (modelItem instanceof OutBus bus) {
                            result += ":mask" + bus.mask;
                        }
                        return result;
                    });
            if (mergedStream == null) {
                mergedStream = itemStream;
            } else {
                mergedStream = Stream.concat(mergedStream, itemStream);
            }
        }
        if (mergedStream != null) {
            return mergedStream.sorted()
                    .collect(Collectors.joining(";"));
        }
        return "";
    }

    public static int countLeadingSpaces(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() - text.stripLeading().length();
    }

    public static String regexEscape(String input) {
        input = input.replace("\\s", " ");
        String[] specialChars = {"\\", "^", "$", ".", "|", "?", "*", "+", "(", ")", "[", "{", "]", "}", ":", "!"};
        for (String ch : specialChars) {
            input = input.replace(ch, "\\" + ch);
        }
        return input;
    }

    public static <T> boolean notContain(T[] array, T item) {
        if (array != null) {
            for (T t : array) {
                if (t.equals(item)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void getOptimised(ModelItem<?>[] array, ModelItem<?> source) {
        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].getOptimised(source);
        }
    }
}
