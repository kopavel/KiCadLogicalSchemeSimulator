/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.tools;
import pko.KiCadLogicalSchemeSimulator.api.ModelItem;
import pko.KiCadLogicalSchemeSimulator.api.bus.OutBus;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"unused", "StaticMethodOnlyUsedInOneClass"})
public enum Utils {
    ;

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
            if (!content.isEmpty()) {
                content.append(System.lineSeparator());
            }
            content.append(line);
        }
        return content.toString();
    }

    public static <T> T[] addToArray(T[] array, T item) {
        int length = array.length;
        T[] newArray = Arrays.copyOf(array, length + 1);
        newArray[length] = item;
        return newArray;
    }

    public static int getMaskForSize(int size) {
        return Math.toIntExact((1L << size) - 1);
    }

    @SafeVarargs
    public static String getHash(Collection<? extends ModelItem<?>>... items) {
        return Arrays.stream(items).flatMap(Collection::stream)
                .map(modelItem -> {
                    String result = modelItem.getName();
                    if (modelItem instanceof OutBus bus) {
                        result += ":mask" + bus.mask;
                    }
                    return result;
                }).sorted()
                .collect(Collectors.joining(";"));
    }

    public static int countLeadingSpaces(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() - text.stripLeading().length();
    }

    public static String regexEscape(String input) {
        StringBuilder result = new StringBuilder(input.replace("\\s", " "));
        String[] specialChars = {"\\", "^", "$", ".", "|", "?", "*", "+", "(", ")", "[", "{", "]", "}", ":", "!"};
        for (String s : specialChars) {
            int index = 0;
            while ((index = result.indexOf(s, index)) != -1) {
                result.insert(index, "\\");
                index += 2;
            }
        }
        return result.toString();
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

    public static String LPad(int i, char c, String string) {
        int pad = i - string.length();
        if (pad <= 0) {
            return string;
        }
        return Character.toString(c).repeat(pad) + string;
    }

    public static final class AlphanumericComparator implements Comparator<String> {
        public static <T> Comparator<T> comparing(Function<? super T, String> keyExtractor) {
            return Comparator.comparing(keyExtractor, new AlphanumericComparator());
        }

        @Override
        public int compare(String s1, String s2) {
            int i = 0, j = 0;
            int len1 = s1.length();
            int len2 = s2.length();
            while (i < len1 && j < len2) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(j);
                if (isDigit(c1) && isDigit(c2)) {
                    int startI = i;
                    int startJ = j;
                    while (i < len1 && isDigit(s1.charAt(i))) {
                        i++;
                    }
                    while (j < len2 && isDigit(s2.charAt(j))) {
                        j++;
                    }
                    String num1 = s1.substring(startI, i);
                    String num2 = s2.substring(startJ, j);
                    int cmp = Integer.compare(num1.length(), num2.length());
                    if (cmp != 0) {
                        return cmp;
                    }
                    cmp = num1.compareTo(num2);
                    if (cmp != 0) {
                        return cmp;
                    }
                } else {
                    if (c1 != c2) {
                        return Character.compare(c1, c2);
                    }
                    i++;
                    j++;
                }
            }
            return Integer.compare(len1, len2);
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }
    }
}
