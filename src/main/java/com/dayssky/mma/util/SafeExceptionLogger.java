package com.dayssky.mma.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeExceptionLogger {
    private static final SystemToastId EXCEPTION_TOAST = new SystemToastId(10000L);
    public static final int INITIAL_COOLDOWN = 10000;
    public static final int COOLDOWN_RESET = 300000;
    private long nextLogTime = 0L;
    private double cooldownScaleFactor = 1.0;
    private final Logger logger;

    public SafeExceptionLogger(Logger logger) {
        this.logger = logger;
    }

    public SafeExceptionLogger(String name) {
        this(LoggerFactory.getLogger(name));
    }

    public void runSafely(Runnable task) {
        this.runSafely(task, null);
    }

    public <T> Optional<T> runSafely(Supplier<T> task) {
        return this.runSafely(task, null);
    }

    public void runSafely(Runnable task, @Nullable Supplier<String> context) {
        this.runSafely(() -> {
            task.run();
            return Unit.INSTANCE;
        }, context);
    }

    public void onException(Throwable e, @Nullable String context) {
        long currentTime = System.currentTimeMillis();
        if (currentTime > this.nextLogTime) {
            if (currentTime - this.nextLogTime > 300000L) {
                this.cooldownScaleFactor = 1.0;
            }

            this.nextLogTime = currentTime + (long) (10000.0 * this.cooldownScaleFactor);
            if (this.cooldownScaleFactor < 20.0) {
                this.cooldownScaleFactor *= 1.5;
            }

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            if (context != null) {
                this.logger.error("context for next exception: {}", context);
                pw.println("context = " + context);
            }

            this.logger.error("uncaught exception: ", e);
            e.printStackTrace(pw);
            Minecraft.getInstance()
                    .getToasts()
                    .addToast(
                            new SystemToast(
                                    EXCEPTION_TOAST,
                                    Component.translatable("text.mma.toast.uncaught_exception.title"),
                                    Component.translatable("text.mma.toast.uncaught_exception.message")
                            )
                    );

            try {
                ChatUtil.sendWarn(
                        Component.translatable("text.mma.toast.uncaught_exception.message_copy_data")
                                .withStyle(s -> s.withClickEvent(new ClickEvent(Action.COPY_TO_CLIPBOARD, sw.toString())))
                );
            } catch (Throwable var8) {
            }
        }
    }

    public <T> Optional<T> runSafely(Supplier<T> supplier, @Nullable Supplier<String> context) {
        try {
            return Optional.of(supplier.get());
        } catch (Throwable var4) {
            this.onException(var4, context == null ? null : context.get());
            return Optional.empty();
        }
    }
}
