package com.floweytf.fma.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class Spinlock {
    private static final VarHandle HANDLE;

    static {
        VarHandle handle;
        try {
            handle = MethodHandles.lookup().findVarHandle(Spinlock.class, "value", int.class);
        } catch (ReflectiveOperationException e) {
            handle = Util.sneakyThrow(e);
        }
        HANDLE = handle;
    }

    private volatile int value;

    @SuppressWarnings("StatementWithEmptyBody")
    public void lock() {
        while (true) {
            if ((int) HANDLE.getAndSetAcquire(this, 1) == 0) {
                return;
            }

            while ((int) HANDLE.get(value) == 1) {
            }
        }
    }

    public void unlock() {
        HANDLE.setRelease(this, 0);
    }
}
