This file captures some benchmark results. The intention is that this file can be updated as optimisations are applied,
to proved a record of performance.

## Benchmarks

### Method calls benchmark

Approx 8 x 16 = 128 unique method calls, 20 iterations. The idea is to show how speed improves on later iterations due
to reinstrumenting the classes after they've been called.

Note: large numbers of empty methods are generated with `python scripts/generate_benchmark_method_classes.py`.

#### No agent installed
```
Initial class load time: 407455 ns (0.407 ms)
Iteration 1: 2478115 ns (2.478 ms)
Iteration 2: 14806 ns (0.015 ms)
Iteration 3: 13759 ns (0.014 ms)
Iteration 4: 13829 ns (0.014 ms)
Iteration 5: 14736 ns (0.015 ms)
Iteration 6: 13759 ns (0.014 ms)
Iteration 7: 13899 ns (0.014 ms)
Iteration 8: 14247 ns (0.014 ms)
Iteration 9: 13899 ns (0.014 ms)
Iteration 10: 14318 ns (0.014 ms)
Iteration 11: 14317 ns (0.014 ms)
Iteration 12: 14387 ns (0.014 ms)
Iteration 13: 14248 ns (0.014 ms)
Iteration 14: 14388 ns (0.014 ms)
Iteration 15: 14108 ns (0.014 ms)
Iteration 16: 13968 ns (0.014 ms)
Iteration 17: 14666 ns (0.015 ms)
Iteration 18: 14457 ns (0.014 ms)
Iteration 19: 14178 ns (0.014 ms)
Iteration 20: 14457 ns (0.014 ms)
Total time for callAllMethods: 3136091 ns (3.136 ms)
```

#### With agent installed
```
Initial class load time: 9407994 ns (9.408 ms)
Iteration 1: 3238280659 ns (3238.281 ms)
Iteration 2: 225658 ns (0.226 ms)
Iteration 3: 30032 ns (0.030 ms)
Iteration 4: 29543 ns (0.030 ms)
Iteration 5: 28565 ns (0.029 ms)
Iteration 6: 29543 ns (0.030 ms)
Iteration 7: 29054 ns (0.029 ms)
Iteration 8: 29054 ns (0.029 ms)
Iteration 9: 29054 ns (0.029 ms)
Iteration 10: 28985 ns (0.029 ms)
Iteration 11: 29543 ns (0.030 ms)
Iteration 12: 29543 ns (0.030 ms)
Iteration 13: 28635 ns (0.029 ms)
Iteration 14: 30591 ns (0.031 ms)
Iteration 15: 29473 ns (0.029 ms)
Iteration 16: 28635 ns (0.029 ms)
Iteration 17: 28215 ns (0.028 ms)
Iteration 18: 28705 ns (0.029 ms)
Iteration 19: 29333 ns (0.029 ms)
Iteration 20: 29473 ns (0.029 ms)
Total time for callAllMethods: 3249799751 ns (3249.800 ms)
``````

### Simple app benchmark

An app with a few (less than a dozen) method calls.

#### No agent installed
```
SampleApp main finished in 0 ms
```

#### With agent installed
```
SampleApp main finished in 76 ms
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