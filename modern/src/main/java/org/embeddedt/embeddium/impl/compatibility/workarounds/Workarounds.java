package org.embeddedt.embeddium.impl.compatibility.workarounds;

//? if >=1.18 {
import org.embeddedt.embeddium.impl.compatibility.environment.OSInfo;
import org.embeddedt.embeddium.impl.compatibility.environment.probe.GraphicsAdapterInfo;
import org.embeddedt.embeddium.impl.compatibility.environment.probe.GraphicsAdapterProbe;
import org.embeddedt.embeddium.impl.compatibility.environment.probe.GraphicsAdapterVendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.embeddedt.embeddium.api.EmbeddiumConstants.MODNAME;

public class Workarounds {
    private static final Logger LOGGER = LoggerFactory.getLogger(MODNAME + "-Workarounds");

    private static final AtomicReference<Set<Reference>> ACTIVE_WORKAROUNDS = new AtomicReference<>(EnumSet.noneOf(Reference.class));

    public static void init() {
        var workarounds = findNecessaryWorkarounds();

        if (!workarounds.isEmpty()) {
            LOGGER.warn("Embeddium has applied one or more workarounds to prevent crashes or other issues on your system: [{}]",
                    workarounds.stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
            LOGGER.warn("This is not necessarily an issue, but it may result in certain features or optimizations being " +
                    "disabled. You can sometimes fix these issues by upgrading your graphics driver.");
        }

        ACTIVE_WORKAROUNDS.set(workarounds);
    }

    private static Set<Reference> findNecessaryWorkarounds() {
        var workarounds = EnumSet.noneOf(Reference.class);
        var operatingSystem = OSInfo.getOS();

        var graphicsAdapters = GraphicsAdapterProbe.getAdapters();

        if (isUsingNvidiaGraphicsCard(operatingSystem, graphicsAdapters)) {
            workarounds.add(Reference.NVIDIA_THREADED_OPTIMIZATIONS);
        }

        if (operatingSystem == OSInfo.OS.LINUX) {
            var session = System.getenv("XDG_SESSION_TYPE");

            if (session == null) {
                LOGGER.warn("Unable to determine desktop session type because the environment variable XDG_SESSION_TYPE " +
                        "is not set! Your user session may not be configured correctly.");
            }

            if (Objects.equals(session, "wayland")) {
                // This will also apply under Xwayland, even though the problem does not happen there
                workarounds.add(Reference.NO_ERROR_CONTEXT_UNSUPPORTED);
            }
        }

        return Collections.unmodifiableSet(workarounds);
    }

    private static boolean isUsingNvidiaGraphicsCard(OSInfo.OS operatingSystem, Collection<GraphicsAdapterInfo> adapters) {
        return (operatingSystem == OSInfo.OS.WINDOWS || operatingSystem == OSInfo.OS.LINUX) &&
                adapters.stream().anyMatch(adapter -> adapter.vendor() == GraphicsAdapterVendor.NVIDIA);
    }

    public static boolean isWorkaroundEnabled(Reference id) {
        return ACTIVE_WORKAROUNDS.get()
                .contains(id);
    }

    public enum Reference {
        /**
         * The NVIDIA driver applies "Threaded Optimizations" when Minecraft is detected, causing severe
         * performance issues and crashes.
         * <a href="https://github.com/CaffeineMC/sodium-fabric/issues/1816">GitHub Issue</a>
         */
        NVIDIA_THREADED_OPTIMIZATIONS,

        /**
         * Requesting a No Error Context causes a crash at startup when using a Wayland session.
         * <a href="https://github.com/CaffeineMC/sodium-fabric/issues/1624">GitHub Issue</a>
         */
        NO_ERROR_CONTEXT_UNSUPPORTED,
    }
}

//?}