// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.transformer;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import nz.rd.nonop.config.ScanConfig;
import nz.rd.nonop.internal.config.ScanMatcher;
import nz.rd.nonop.internal.NonopStaticHooks;
import nz.rd.nonop.internal.util.NonopLogger;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class NonopClassfileTransformer implements ClassFileTransformer {

    public interface GetMethodUsageSnapshot {
        Set<Pair<String, String>> usageSnapshotForInstrumentation(Class<?> clazz);
    }

    private final GetMethodUsageSnapshot usageSnapshot;
    private final NonopLogger nonopLogger;

    // Pre-compile matchers for efficiency
    private final net.bytebuddy.matcher.ElementMatcher<TypeDescription> typeMatcher;
    private final net.bytebuddy.matcher.ElementMatcher<MethodDescription> methodMatcher;

    private final boolean scanIncludeBootstrap;
    private final boolean scanIncludeUnnamed;

    public NonopClassfileTransformer(ScanConfig scanConfig, GetMethodUsageSnapshot usageSnapshot, NonopLogger nonopLogger) {

        this.usageSnapshot = usageSnapshot;
        this.nonopLogger = nonopLogger;

        ElementMatcher.Junction<TypeDescription> typeMatcherTemp =
                ElementMatchers.isSubTypeOf(Object.class)
                .or(ElementMatchers.isInterface());

        if (!scanConfig.isScanIncludeSynthetic()) {
            typeMatcherTemp = typeMatcherTemp.and(ElementMatchers.not(ElementMatchers.isSynthetic()));
        }

        List<ScanMatcher> allScanMatchers = new ArrayList<>();
        allScanMatchers.addAll(scanConfig.getBuiltinScanMatchers());
        allScanMatchers.addAll(scanConfig.getUserScanMatchers());
        nonopLogger.debug("[nonop-config] Loaded scan matchers: " + allScanMatchers);
        NameBasedScanRuleMatcher nameBasedScanRuleMatcher = new NameBasedScanRuleMatcher(allScanMatchers, nonopLogger);
        typeMatcherTemp = typeMatcherTemp.and(nameBasedScanRuleMatcher);

        // The NameBasedScanRuleMatcher now incorporates all include/exclude logic based on the ordered list.
        this.typeMatcher = typeMatcherTemp;

        this.scanIncludeBootstrap = scanConfig.isScanIncludeBootstrap();
        this.scanIncludeUnnamed = scanConfig.isScanIncludeUnnamed();

        // Updated method matcher to include default methods and static methods in interfaces
        this.methodMatcher = ElementMatchers.isMethod()
                .and(ElementMatchers.not(ElementMatchers.isAbstract()))
                .and(ElementMatchers.not(ElementMatchers.isNative()))
                .and(ElementMatchers.not(ElementMatchers.isBridge()))
                .and(ElementMatchers.not(ElementMatchers.isSynthetic()))
                .and(ElementMatchers.not(ElementMatchers.nameStartsWith("lambda$"))) // Exclude lambda methods // TODO: are these synthetic anyway?
                .or(ElementMatchers.isConstructor())
                .or(ElementMatchers.isDefaultMethod()) // Include interface default methods
                .or(ElementMatchers.isStatic().and(ElementMatchers.isMethod())); // Include static methods in interfaces
    }

    // TODO: Check if this method can be called concurrently for the same class and ensure correctness
    @Override
    public byte[] transform(
            ClassLoader loader,
            String classNameJVM,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) throws IllegalClassFormatException {
//        nonopLogger.debug("Transform called for: " + classNameJVM + ", loader: " + loader);
        // TODO: Consider different classloaders may define different classes
        // TODO: Consider classloaders may be unloaded, perhaps need to allow related instrumentation to be unloaded
        // TODO: If we unload anything, need to make sure we don't lose any recorded method usages before we unload it
        // TODO: Thinka about Modules and ProectionDomains

        try {
            // Not documented in Javadoc, but have observed that class names can be null somtimes
            // This is apparently due to synthetic lambda generation
            // See:
            // - https://github.com/puniverse/quasar/issues/160
            // - https://hibernate.atlassian.net/browse/HHH-9541
            if (!this.scanIncludeUnnamed && classNameJVM == null) {
                // TODO: Consider if we want to allow this - would be hard to record usage, technically live code could be in a lambda
//                nonopLogger.debug("Skipping instrumentation for unnamed synthetic class in ClassLoader: " + loader);
                return null;
            }

            // TODO: Filter on classnames here before running other code - by excluding java/* early we can also exclude lots of weird synthetic classes, etc
            // TODO: Add option to include boot classloader - excludes lots of java and weird classes
            // Example unusual class: net.bytebuddy.pool.TypePool$Resolution$NoSuchTypeException: Cannot resolve type description for java.lang.invoke.BoundMethodHandle$Species_L4
            if (!this.scanIncludeBootstrap && loader == null) {
//                nonopLogger.debug("Skipping instrumentation for bootstrap ClassLoader for className: " + classNameJVM);
                return null;
            }

            // Example transformation: java/lang/invoke/BoundMethodHandle$Species_L4 ->
            // net.bytebuddy.pool.TypePool$Resolution$NoSuchTypeException: Cannot resolve type description for java.lang.invoke.BoundMethodHandle$Species_L4
            String canonicalClassName = classNameJVM.replace('/', '.');

            // Create TypeDescription based on whether the class is being redefined or initially loaded
            TypeDescription typeDescription;
            Set<Pair<String, String>> usedMethods;
            if (classBeingRedefined != null) {
                // For retransformation, use the loaded class
                typeDescription = new TypeDescription.ForLoadedType(classBeingRedefined);
                // TODO: Verify that the JVM guarantees no concurrent retransformations, otherwise we could revert implementation (but would only be a perf loss / re-logged usage?)
                usedMethods = usageSnapshot.usageSnapshotForInstrumentation(classBeingRedefined);
            } else {
                // For initial load, use TypePool to resolve from JVM internal name
                TypePool typePool = loader != null ? TypePool.Default.of(loader) : TypePool.Default.ofBootLoader();
                typeDescription = typePool.describe(canonicalClassName).resolve();
                usedMethods = Collections.emptySet(); // New class definition - cannot have been used; use singleton empty set; avoid creating objects yet
            }

            if (!typeMatcher.matches(typeDescription)) {
//                nonopLogger.debug("Skipping transformation for excluded class named: " + canonicalClassName + ", loader: " + loader);
                return null; // Do not transform
            }

            nonopLogger.debug("Transforming class: " + canonicalClassName +
                    (classBeingRedefined != null ? " (redefining)" : " (initial)") + " for ClassLoader: " + loader + ". Used methods: " + usedMethods);

            return instrumentUnusedMethods(typeDescription, canonicalClassName, classfileBuffer, usedMethods);

        } catch (Exception e) {
            nonopLogger.error("Exception during transform for class: " + classNameJVM, e);
            return null;
        }
    }

    // Public for testing or direct use
    public byte @Nullable [] instrumentUnusedMethods(TypeDescription typeDescription, String canonicalClassName, byte[] classfileBuffer, Set<Pair<String, String>> usedMethods) {
        // TODO: If this code can be called concurrently for a class we are entering a race at this point which could result in incorrect instrumentation if ordering is reversed
        // TODO: Double check if we should be using something like AgentBuilder.disableClassFormatChanges to ensure we're doing conservative/low impact changes to classes
        DynamicType.Builder<?> builder = new ByteBuddy()
                // TODO: Get canonicalClassNameFrom typeDescription to avoid arg?
                .redefine(typeDescription, ClassFileLocator.Simple.of(canonicalClassName, classfileBuffer));

        boolean changed = false;
        List<MethodDescription.InDefinedShape> methods = typeDescription.getDeclaredMethods().stream()
            .filter(methodMatcher::matches)
            .collect(Collectors.toList());

        // Print used methods for debugging
//        nonopLogger.debug("Used methods for " + canonicalClassName + ": " + usedMethods);

        for (MethodDescription.InDefinedShape method : methods) {
            // TODO: Decide on which string representation for usage tracking is the fastest and most useful
            String methodName = method.getInternalName(); // Method name or <init>
            String methodDescriptor = method.getDescriptor();
            Pair<String, String> methodKey = ImmutablePair.of(methodName, methodDescriptor);

//            nonopLogger.debug("Processing method: " + methodName + " " + methodDescriptor);

            // Used methods should be an empty set if this hasn't been called yet
            boolean shouldInstrumentThisMethod = !usedMethods.contains(methodKey);

            if (shouldInstrumentThisMethod) {
                // This method has not been called yet, so instrument it to call the hook
                nonopLogger.debug("Method transformation: " + canonicalClassName + " " + methodName + " " + methodDescriptor + ": UNUSED - instrumenting");
                // TODO: Consider micro-optimisations like caching Advice object
                builder = builder.visit(Advice.to(CallMethodCalledHook.class).on(ElementMatchers.is(method)));
                changed = true;
            } else {
                // By not transforming this method, we are not generating instrumentation for this method.
                // If the method was previously instrumented, this effectively strips the instrumentation, making
                // future method calls zero overhead.
                nonopLogger.debug("Method transformation: " + canonicalClassName + " " + methodName + " " + methodDescriptor + ": ALREADY USED - skipping");
            }
        }

        if (changed) {
            nonopLogger.debug("Applying changes to: " + canonicalClassName);
            // builder.make().saveIn(new File("./dump")); - dump to analyse, use javap -v -constants -c -classpath dump nz.rd.nonoptest.integration.SampleInterface
            return builder.make().getBytes();
        } else {
            nonopLogger.debug("No changes needed for: " + canonicalClassName);
            // TODO: RecorderAPI.allMethodsUsed() - can possibly free any structures taken to record usage now, esp if have recorded some usage already
            return null;
        }
    }

    public static class CallMethodCalledHook {
        @Advice.OnMethodEnter(suppress = Throwable.class) // TODO: Remove suppression if generates try/catch bytecode
        public static void enter(
                @Advice.Origin Class<?> clazz,
                @Advice.Origin("#m") String methodName,
                @Advice.Origin("#d") String methodDescriptor
        ) {
            // TODO: Check bytecode generated is minimised
            NonopStaticHooks.methodCalled(clazz, methodName, methodDescriptor);
        }
    }

}