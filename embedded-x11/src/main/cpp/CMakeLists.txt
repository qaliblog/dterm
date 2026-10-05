cmake_minimum_required(VERSION 3.22)
project(ldfa_embedded_x11)

# The standalone dterm build no longer carries the legacy vendor/termux-x11
# Termux:X11 integration.  This module exposes the minimal ABI the dterm-native
# Android layer depends on (the libXlorie shared library) so the release APK ships
# the bundled X11 runtime alongside the Debian rootfs.  All native translation units
# here are self-contained and do not read the removed vendor/termux-x11 tree.

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(xlorie SHARED
    Xlorie.cpp
)

# Package for the NDK build.  Use the Android target triplet so the resulting
# libXlorie.so is a proper Android native library.
set_target_properties(xlorie PROPERTIES
    OUTPUT_NAME "Xlorie"
    PREFIX "lib"
    SUFFIX ".so"
)

# Ensure the native library is included in the AAR/APR packaging of this module.
# The :app module depends on :embedded-x11, so Android's packaging will fold
# libXlorie.so into the app APK for each configured ABI.
