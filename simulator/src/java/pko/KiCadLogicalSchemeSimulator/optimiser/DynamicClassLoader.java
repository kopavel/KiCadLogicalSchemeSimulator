/*
 *
 *  * Copyright (C) 2024 Pavel Korzh
 *  * SPDX-License-Identifier: GPL-3.0-only
 *
 */
package pko.KiCadLogicalSchemeSimulator.optimiser;
public class DynamicClassLoader extends ClassLoader {
    public DynamicClassLoader(ClassLoader parent) {
        super(parent);
    }

    // Define the class in the desired package
    public Class<?> defineClassInPackage(String className, byte[] byteCode) {
        return defineClass(className, byteCode, 0, byteCode.length);
    }
}
