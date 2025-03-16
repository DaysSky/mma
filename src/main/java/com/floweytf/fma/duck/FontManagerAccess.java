package com.floweytf.fma.duck;

public interface FontManagerAccess {
    default long fma$getReloadCounter() {
        throw new AbstractMethodError();
    }
}
