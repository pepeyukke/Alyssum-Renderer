package org.embeddedt.embeddium.impl.compatibility.workarounds.nvidia;

//? if >=1.18 {
import org.embeddedt.embeddium.impl.compatibility.environment.OSInfo;
import org.embeddedt.embeddium.impl.platform.unix.Libc;
import org.embeddedt.embeddium.impl.platform.windows.api.Kernel32;
import org.embeddedt.embeddium.impl.platform.windows.WindowsCommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.embeddedt.embeddium.api.EmbeddiumConstants.MODNAME;

public class NvidiaWorkarounds {
    private static final Logger LOGGER = LoggerFactory.getLogger(MODNAME + "-NvidiaWorkarounds");

    public static void install() {
        LOGGER.warn("Applying workaround: Prevent the NVIDIA OpenGL driver from using broken optimizations (NVIDIA_THREADED_OPTIMIZATIONS)");

        try {
            switch (OSInfo.getOS()) {
                case WINDOWS -> {
                    // The NVIDIA drivers rely on parsing the command line arguments to detect Minecraft. If we destroy those,
                    // then it shouldn't be able to detect us anymore.
                    WindowsCommandLine.setCommandLine("net.caffeinemc.sodium");

                    // Ensures that Minecraft will run on the dedicated GPU, since the drivers can no longer detect it
                    Kernel32.setEnvironmentVariable("SHIM_MCCOMPAT", "0x800000001");
                }
                case LINUX -> {
                    // Unlike Windows, we don't need to hide ourselves from the driver. We can just request that
                    // it not use threaded optimizations instead.
                    Libc.setEnvironmentVariable("__GL_THREADED_OPTIMIZATIONS", "0");
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Failure while applying workarounds", t);

            LOGGER.error("READ ME! The workarounds for the NVIDIA Graphics Driver did not apply correctly!");
            LOGGER.error("READ ME! You are very likely going to run into unexplained crashes and severe performance issues!");
            LOGGER.error("READ ME! Please see this issue for more information: https://github.com/CaffeineMC/sodium-fabric/issues/1816");
        }
    }

    public static void uninstall() {
        switch (OSInfo.getOS()) {
            case WINDOWS -> {
                WindowsCommandLine.resetCommandLine();
            }
            case LINUX -> { }
        }
    }
}
//?}