package com.floweytf.fma.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import org.jetbrains.annotations.NotNull;

public class LateInit<T> implements Supplier<T> {
    private T value;

    public void init(@NotNull T init) {
        Preconditions.checkState(this.value == null, "init() called on LateInit twice");
        Preconditions.checkNotNull(init);
        this.value = init;
    }

    public void tryInit(@NotNull T init) {
        if (!this.isInit()) {
            this.init(init);
        }
    }

    public boolean isInit() {
        return this.value != null;
    }

    public T get() {
        Preconditions.checkState(this.value != null, "get() called before init()");
        return this.value;
    }
}
