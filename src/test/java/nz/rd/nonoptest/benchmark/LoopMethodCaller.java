package nz.rd.nonoptest.benchmark;

import nz.rd.nonoptest.benchmark.generated.MethodCaller;

public class LoopMethodCaller {
    // Provide an efficient entry point into the loop
    // This lets the loop be triggered by a single reflective call
    // Callers will often use reflection to delay classloading times, but we don't want to call a reflected
    // method in the tight loop if possible
    public static void callAllMethodsMultipleTimes(int iterations, long callStartTimeNanos, long[] iterationTimes) {
        long callEndTimeNanos = System.nanoTime();
        // TODO: Log how long call took? (Maybe doesn't matter as MethodCaller.callAllMethods() invocation below probably triggers actual load)

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            MethodCaller.callAllMethods();
            long end = System.nanoTime();
            iterationTimes[i] = end - start;
        }
    }
}
