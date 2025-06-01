// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal;

// TODO: Instead of having a global hook, add one per ClassLoader to (a) make lookup fast and (b) avoid leaks
public final class NonopStaticHooks {

    private static MethodCalled methodCalled;

    public static void initialize(MethodCalled methodCalled) {
        NonopStaticHooks.methodCalled = methodCalled;
    }

    // Called by instrumented code (Phase 0)
    public static void methodCalled(Class<?> clazz, String methodSignature) {
        methodCalled.methodCalled(clazz, methodSignature);
    }

    public static interface MethodCalled {
        // TODO: Change to take methodName ("method") and methodDescriptor ("()V") separately since they are already stored in classfile constants, so we can reuse them and reduce memory usage
        void methodCalled(Class<?> clazz, String methodSignature);
    }
}