This file captures some benchmark results. The intention is that this file can be updated as optimisations are applied,
to proved a record of performance.

## Benchmarks

### Method calls benchmark

Approx 8 x 16 = 128 unique method calls, 20 iterations. The idea is to show how speed improves on later iterations due
to reinstrumenting the classes after they've been called.

Note: large numbers of empty methods are generated with `python scripts/generate_benchmark_method_classes.py`.

#### No agent installed
```
Initial class load time: 288380 ns (0.288 ms)
Iteration 1: 2334563 ns (2.335 ms)
Iteration 2: 14248 ns (0.014 ms)
Iteration 3: 13829 ns (0.014 ms)
Iteration 4: 13829 ns (0.014 ms)
Iteration 5: 13828 ns (0.014 ms)
Iteration 6: 13480 ns (0.013 ms)
Iteration 7: 13759 ns (0.014 ms)
Iteration 8: 13968 ns (0.014 ms)
Iteration 9: 14038 ns (0.014 ms)
Iteration 10: 13340 ns (0.013 ms)
Iteration 11: 14038 ns (0.014 ms)
Iteration 12: 13829 ns (0.014 ms)
Iteration 13: 13829 ns (0.014 ms)
Iteration 14: 14318 ns (0.014 ms)
Iteration 15: 13829 ns (0.014 ms)
Iteration 16: 13759 ns (0.014 ms)
Iteration 17: 13828 ns (0.014 ms)
Iteration 18: 13759 ns (0.014 ms)
Iteration 19: 13829 ns (0.014 ms)
Iteration 20: 13340 ns (0.013 ms)
Total time for callAllMethods: 2963287 ns (2.963 ms)
```

#### With agent installed
```
Initial class load time: 9360041 ns (9.360 ms)
Iteration 1: 136667953 ns (136.668 ms)
Iteration 2: 140348537 ns (140.349 ms)
Iteration 3: 80459 ns (0.080 ms)
Iteration 4: 13829 ns (0.014 ms)
Iteration 5: 13270 ns (0.013 ms)
Iteration 6: 12920 ns (0.013 ms)
Iteration 7: 12851 ns (0.013 ms)
Iteration 8: 12502 ns (0.013 ms)
Iteration 9: 13270 ns (0.013 ms)
Iteration 10: 13968 ns (0.014 ms)
Iteration 11: 13200 ns (0.013 ms)
Iteration 12: 13899 ns (0.014 ms)
Iteration 13: 13689 ns (0.014 ms)
Iteration 14: 13479 ns (0.013 ms)
Iteration 15: 13270 ns (0.013 ms)
Iteration 16: 13200 ns (0.013 ms)
Iteration 17: 13829 ns (0.014 ms)
Iteration 18: 12711 ns (0.013 ms)
Iteration 19: 12991 ns (0.013 ms)
Iteration 20: 13759 ns (0.014 ms)
Total time for callAllMethods: 277447978 ns (277.448 ms)
``````

### Simple app benchmark

An app with a few (less than a dozen) method calls.

#### No agent installed
```
SampleApp main finished in 0 ms
```

#### With agent installed
```
SampleApp main finished in 24 ms
```

## Methodology

Benchmarks were performed on a dev environment and are not intended to be particularly accurate. They're just designed
to highlight areas to give rough orders of magnitude and identity areas to look at optimising.

Tests are run with:
```
$ ./gradlew clean runAllBenchmarks
```

Test machine: AMD Ryzen 5 4500U (6C / 6T, 2.3 / 4.0GHz, 3MB L2 / 8MB L3) laptop CPU
Memory: 24GB

```
$ java -version

java -version
openjdk version "1.8.0_452"
OpenJDK Runtime Environment (Temurin)(build 1.8.0_452-b09)
OpenJDK 64-Bit Server VM (Temurin)(build 25.452-b09, mixed mode)

$ lsb_release -d
No LSB modules are available.
Description:    Ubuntu 24.04.2 LTS

$ uname -a
Linux 6.8.0-58-generic #60-Ubuntu SMP PREEMPT_DYNAMIC Fri Mar 14 18:29:48 UTC 2025 x86_64 x86_64 x86_64 GNU/Linux
```