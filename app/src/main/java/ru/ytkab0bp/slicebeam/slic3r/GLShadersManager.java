// SPDX-License-Identifier: AGPL-3.0-only
package ru.ytkab0bp.slicebeam.slic3r;

/**
 * Headless compatibility shim required by the bundled native library's JNI_OnLoad.
 *
 * The native library resolves this exact class and private static method even when the
 * application does not use the OpenGL model viewer. Keep the method name, visibility,
 * static modifier, and return type aligned with the native JNI contract.
 */
public final class GLShadersManager {
    private GLShadersManager() {
    }

    @SuppressWarnings("unused")
    private static long getCurrentShaderPointer() {
        // This app uses the slicer headlessly; there is no active GL shader to return.
        return 0L;
    }
}
