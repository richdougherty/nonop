
package nz.rd.nonoptest.benchmark;

import java.lang.reflect.Method;

public class BenchmarkMain {
    public static void main(String[] args) throws Exception {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 20;

        ClassLoader classLoader = BenchmarkMain.class.getClassLoader();
        // TODO: If possible, assert that we haven't accidentally loaded the BenchMark main class and triggered loads of the method classes
        // Check classLoader to see if loaded yet

        System.out.println("Loading LoopMethodCaller");
        long initialLoadStartTime = System.nanoTime(); // Initial load of classes will trigger instrumentation
        Class<?> methodCallerClazz = classLoader.loadClass("nz.rd.nonoptest.benchmark.LoopMethodCaller");
        long initialLoadEndTime = System.nanoTime();
        System.out.println("Finished loading LoopMethodCaller");
        long initialLoadTime = initialLoadEndTime - initialLoadStartTime;

        Method callAllMethods = methodCallerClazz.getDeclaredMethod("callAllMethodsMultipleTimes", int.class, long.class, long[].class);
        long[] iterationTimes = new long[iterations];
        //     public static void callAllMethodsMultipleTimes(int iterations, long[] iterationTimes) {
        long callAllStartTime = System.nanoTime();
        callAllMethods.invoke(null, iterations, callAllStartTime, iterationTimes);
        long callAllEndTime = System.nanoTime();
        long callAllTime = callAllEndTime - callAllStartTime;

        // Print load time, time for each iteration, etc
        System.out.printf("%n%n");
        System.out.printf("Initial class load time: %d ns (%.3f ms)%n",
            initialLoadTime, initialLoadTime / 1_000_000.0);
        for (int i = 0; i < iterations; i++) {
            System.out.printf("Iteration %d: %d ns (%.3f ms)%n",
                i + 1, iterationTimes[i], iterationTimes[i] / 1_000_000.0);
        }
        System.out.printf("Total time for callAllMethods: %d ns (%.3f ms)%n",
                callAllTime, callAllTime / 1_000_000.0);
    }
}
