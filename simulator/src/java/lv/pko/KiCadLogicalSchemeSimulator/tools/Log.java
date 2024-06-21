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
package lv.pko.KiCadLogicalSchemeSimulator.tools;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Log {
    private static final Map<Class<?>, org.apache.logging.log4j.Logger> loggers = new ConcurrentHashMap<>();

    private Log() {
    }

    public static void error(Class<?> clazz, String message, Object... o) {
        getLogger(clazz).error(message, o);
    }

    public static void error(Class<?> clazz, String message, Supplier<?>... o) {
        getLogger(clazz).error(message, t(o));
    }

    public static void error(Class<?> clazz, String message) {
        getLogger(clazz).error(message);
    }

    public static void error(Class<?> clazz, String message, Throwable t) {
        getLogger(clazz).error(message, t);
    }

    public static void warn(Class<?> clazz, String message, Object... o) {
        getLogger(clazz).warn(message, o);
    }

    public static void warn(Class<?> clazz, String message, Supplier<?>... o) {
        getLogger(clazz).warn(message, t(o));
    }

    public static void warn(Class<?> clazz, String message) {
        getLogger(clazz).warn(message);
    }

    public static void warn(Class<?> clazz, String message, Throwable t) {
        getLogger(clazz).warn(message, t);
    }

    public static void info(Class<?> clazz, String message, Object... o) {
        getLogger(clazz).info(message, o);
    }

    public static void info(Class<?> clazz, String message, Supplier<?>... o) {
        getLogger(clazz).info(message, 0, t(o));
    }

    public static void info(Class<?> clazz, String message) {
        getLogger(clazz).info(message);
    }

    public static void info(Class<?> clazz, String message, Throwable t) {
        getLogger(clazz).info(message, t);
    }

    public static void debug(Class<?> clazz, String message, Object... o) {
        getLogger(clazz).debug(message, o);
    }

    public static void debug(Class<?> clazz, String message, Supplier<?>... o) {
        getLogger(clazz).debug(message, t(o));
    }

    public static void debug(Class<?> clazz, String message) {
        getLogger(clazz).debug(message);
    }

    public static void debug(Class<?> clazz, String message, Throwable t) {
        getLogger(clazz).debug(message, t);
    }

    public static void trace(Class<?> clazz, String message, Object... o) {
        getLogger(clazz).trace(message, o);
    }

    public static void trace(Class<?> clazz, String message, Supplier<?>... o) {
        getLogger(clazz).trace(message, t(o));
    }

    public static void trace(Class<?> clazz, String message) {
        getLogger(clazz).trace(message);
    }

    public static void trace(Class<?> clazz, String message, Throwable t) {
        getLogger(clazz).trace(message, t);
    }

    public static boolean isDebugEnabled(Class<?> clazz) {
        return isEnabled(clazz, Level.DEBUG);
    }

    public static boolean isEnabled(Class<?> clazz, Level level) {
        return getLogger(clazz).isEnabled(level);
    }

    public static boolean isDebugEnabled(Object obj) {
        return isDebugEnabled(obj.getClass());
    }

    public static boolean isEnabled(Object obj, Level level) {
        return isEnabled(obj.getClass(), level);
    }

    private static Logger getLogger(Class<?> clazz) {
        if (!loggers.containsKey(clazz)) {
            synchronized (Log.class) {
                if (!loggers.containsKey(clazz)) {
                    loggers.put(clazz, LogManager.getContext(clazz.getClassLoader(), false).getLogger(clazz));
                }
            }
        }
        return loggers.get(clazz);
    }

    @SuppressWarnings("deprecation")
    private static org.apache.logging.log4j.util.Supplier<?>[] t(java.util.function.Supplier<?>[] suppliers) {
        return Arrays.stream(suppliers)
                .map(old -> (org.apache.logging.log4j.util.Supplier<?>) old::get).toArray(org.apache.logging.log4j.util.Supplier[]::new);
    }
}
