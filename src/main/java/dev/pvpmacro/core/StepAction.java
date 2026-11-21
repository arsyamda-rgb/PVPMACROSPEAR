package dev.pvpmacro.core;

@FunctionalInterface
public interface StepAction<C> {
    void run(C ctx);
}
