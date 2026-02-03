package org.copperforge.mog.runtime;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class MogRuntime {
    private static final AtomicReference<MogRuntimeContext> CONTEXT = new AtomicReference<>();

    private MogRuntime() {
    }

    public static void register(MogRuntimeContext context) {
        CONTEXT.set(context);
    }

    public static void clear() {
        CONTEXT.set(null);
    }

    public static Optional<MogRuntimeContext> current() {
        return Optional.ofNullable(CONTEXT.get());
    }
}
