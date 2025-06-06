// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.pool.TypePool;
import nz.rd.nonop.config.AgentConfig;
import nz.rd.nonop.config.NonopPropertyUtils;
import nz.rd.nonop.internal.transformer.NonopClassfileTransformer;
import nz.rd.nonop.internal.util.NonopConsoleLogger;
import nz.rd.nonop.internal.util.NonopLogger;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;


class NonopClassfileTransformerTest {

    private NonopLogger nonopLogger;
    private AgentConfig agentConfig;
    private NonopClassfileTransformer.GetMethodUsageSnapshot getMethodUsageSnapshot;

    private final AtomicReference<Triple<Class<?>, String, String>> hookArgs = new AtomicReference<>();

    private static final String TEST_CLASS_NAME = "nz.rd.nonoptest.Dynamic1";
    private static final String TEST_METHOD_NAME = "myMethod";
    private static final String TEST_METHOD_DESCRIPTOR = "()V";

    @BeforeEach
    void setUp() throws Exception {
        nonopLogger = new NonopConsoleLogger(false); // Set to true for debugging output from transformer
        agentConfig = AgentConfig.load(nonopLogger, NonopPropertyUtils.loadNonopDefaults());
        getMethodUsageSnapshot = clazz -> Collections.emptySet();

        hookArgs.set(null);

        NonopStaticHooks.MethodCalled methodCalledHook = (clazz, methodName, methodDescriptor) -> {
            hookArgs.set(ImmutableTriple.of(clazz, methodName, methodDescriptor));
            // System.out.println("Hook called: " + clazz.getName() + "#" + methodSignature); // For test debugging
        };
        NonopStaticHooks.initialize(methodCalledHook);
    }

    /**
     * Creates a TypePool capable of resolving both the dynamically generated class
     * and standard JDK/classpath classes.
     */
    private TypePool createComprehensiveTypePool(String dynamicClassName, byte[] dynamicClassBytes) {
        ClassFileLocator dynamicClassLocator = ClassFileLocator.Simple.of(dynamicClassName, dynamicClassBytes);
        ClassFileLocator systemAndClasspathLocator = ClassFileLocator.ForClassLoader.of(getClass().getClassLoader());
        ClassFileLocator compoundLocator = new ClassFileLocator.Compound(dynamicClassLocator, systemAndClasspathLocator);
        return TypePool.Default.of(compoundLocator);
    }

    @Test
    public void instrumentUnusedMethods_shouldInstrumentMethodAndTriggerHook() throws Exception {
        NonopClassfileTransformer transformer = new NonopClassfileTransformer(agentConfig, getMethodUsageSnapshot, nonopLogger);

        // 1. Create original class bytes
        byte[] originalBytes = new ByteBuddy()
                .subclass(Object.class)
                .name(TEST_CLASS_NAME)
                .defineMethod(TEST_METHOD_NAME, void.class, Visibility.PUBLIC)
                .intercept(StubMethod.INSTANCE)
                .make()
                .getBytes();
        assertNotNull(originalBytes);

        // 2. Prepare TypeDescription for the transformer using a comprehensive TypePool
        TypePool typePool = createComprehensiveTypePool(TEST_CLASS_NAME, originalBytes);
        TypeDescription typeDescription = typePool.describe(TEST_CLASS_NAME).resolve();

        assertNotNull(typeDescription, "TypeDescription should be resolved");
        assertNotNull(typeDescription.getSuperClass(), "Superclass TypeDescription should be resolvable");
        assertEquals("java.lang.Object", typeDescription.getSuperClass().asErasure().getName(), "Superclass should be Object");


        // 3. Instrument the class
        byte[] instrumentedBytes = transformer.instrumentUnusedMethods(
                typeDescription,
                TEST_CLASS_NAME,
                originalBytes,
                Collections.emptySet()
        );
        assertThat("Instrumented bytes should not be null", instrumentedBytes, notNullValue());

        // 4. Load the instrumented class
        ClassLoader instrumentedClassLoader = new ByteArrayClassLoader(
                getClass().getClassLoader(), // Parent classloader
                ImmutableMap.of(TEST_CLASS_NAME, instrumentedBytes),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST); // Or other persistence handler
        Class<?> instrumentedClass = instrumentedClassLoader.loadClass(TEST_CLASS_NAME);
        assertNotNull(instrumentedClass, "Instrumented class should be loaded");

        // 5. Instantiate and invoke the method
        Object instance = instrumentedClass.getDeclaredConstructor().newInstance();
        Method method = instrumentedClass.getDeclaredMethod(TEST_METHOD_NAME);
        method.invoke(instance);

        // 6. Assertions
        assertEquals(new ImmutableTriple<>(instrumentedClass, TEST_METHOD_NAME, TEST_METHOD_DESCRIPTOR), hookArgs.get(), "Hook called with correct class and method signature");
    }

    @Test
    public void instrumentUnusedMethods_shouldNotInstrumentAlreadyUsedMethod() throws Exception {
        // Arrange: This time, the method is "already used"
        getMethodUsageSnapshot = clazz -> Collections.singleton(Pair.of(TEST_METHOD_NAME, TEST_METHOD_DESCRIPTOR));
        NonopClassfileTransformer transformer = new NonopClassfileTransformer(agentConfig, getMethodUsageSnapshot, nonopLogger);

        byte[] originalBytes = new ByteBuddy()
                .subclass(Object.class)
                .name(TEST_CLASS_NAME)
                .defineMethod(TEST_METHOD_NAME, void.class, Visibility.PUBLIC)
                .intercept(StubMethod.INSTANCE)
                .make()
                .getBytes();
        assertNotNull(originalBytes);

        TypePool typePool = createComprehensiveTypePool(TEST_CLASS_NAME, originalBytes);
        TypeDescription typeDescription = typePool.describe(TEST_CLASS_NAME).resolve();
        assertNotNull(typeDescription, "TypeDescription should be resolved");

        // Act: Instrument, providing the "used" method signature
        byte[] resultBytes = transformer.instrumentUnusedMethods(
                typeDescription,
                TEST_CLASS_NAME,
                originalBytes,
                ImmutableSet.of(
                        Pair.of("<init>", "()V"),
                        Pair.of(TEST_METHOD_NAME, TEST_METHOD_DESCRIPTOR)) // Mark method as used
        );

        // Assert: No transformation should occur, so resultBytes should be null
        assertThat("Bytes should be null as no transformation is expected", resultBytes, is(nullValue()));

        // To be absolutely sure, if it *were* instrumented, the hook would be called.
        // So, we can also load the original bytes and call the method.
        ClassLoader originalClassLoader = new ByteArrayClassLoader(
                null,
                ImmutableMap.of(TEST_CLASS_NAME, originalBytes),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST
        );
        Class<?> originalLoadedClass = originalClassLoader.loadClass(TEST_CLASS_NAME);
        Object instance = originalLoadedClass.getDeclaredConstructor().newInstance();
        Method method = originalLoadedClass.getDeclaredMethod(TEST_METHOD_NAME);
        method.invoke(instance);

        // Verify hook was NOT called because the method was skipped for instrumentation
        assertNull(hookArgs.get(), "Hook not called");
    }
}