# nonop

Prototype of an extremely low overhead, simple to use Java dead code detector.

1. Attach as an agent - no code changes.
2. Code is instrumented and first use of every method is logged - simple.
3. Instrumentation is removed when not needed, so code runs normally - almost zero overhead.

```
java -javaagent:nonop-agent-<version>.jar com.example.Application
```

## Benchmarks

Testing is at early stages, but simple benchmarks show code runs at normal speed once it's been counted. This is because
the instrumentation bytecode is removed once it's no longer needed, so the code runs in its original, unmodified state.

Example benchmark with 128 empty method calls per iteration.

| Iteration    | Normal (ns) | Normal (ms) | Instrumented (ns) | Instrumented (ms) |
|:-------------|------------:|------------:|------------------:|------------------:|
| Initial      | 308700      | 0.309       | 8878249           | 8.878             |
| Iteration 1  | 2233601     | 2.234       | 155960085         | 155.960           |
| Iteration 2  | 3283        | 0.003       | 108104142         | 108.104           |
| Iteration 3  | 2863        | 0.003       | 47143             | 0.047             |
| Iteration 4  | 2933        | 0.003       | 2794              | 0.003             |
| Iteration 5  | 2863        | 0.003       | 2794              | 0.003             |
| Iteration 6  | 2864        | 0.003       | 45258             | 0.045             |
| Iteration 7  | 2794        | 0.003       | 3353              | 0.003             |
| Iteration 8  | 2863        | 0.003       | 2864              | 0.003             |
| Iteration 9  | 2934        | 0.003       | 2863              | 0.003             |
| Iteration 10 | 3353        | 0.003       | 2933              | 0.003             |
| Iteration 11 | 3422        | 0.003       | 2794              | 0.003             |
| Iteration 12 | 2933        | 0.003       | 2863              | 0.003             |

Output looks something like:
```
nz.rd.nonoptest.benchmark.generated.BenchmarkClass0.method0_0()
nz.rd.nonoptest.benchmark.generated.BenchmarkClass0.method0_1()
...
nz.rd.nonoptest.benchmark.generated.BenchmarkClass7.method7_14()
nz.rd.nonoptest.benchmark.generated.BenchmarkClass7.method7_15()
```

## Usage

Supports Java 8+.

The `nonop` agent runs as a standard JVM agent. By default all your app's code is instrumented and usage information is
printed to stdout.

```
java -javaagent:nonop-agent-<version>.jar com.myapp.MyApp
```

The `nonop.out` property can direct usage logging to a file instead of stdout.
```
java -javaagent:nonop-agent-<version>.jar -Dnonop.out=nonop.log com.myapp.MyApp
```

The `nonop.scan` property can be set to restrict the classes that nonop instruments.
```
java -javaagent:nonop-agent-<version>.jar -Dnonop.scan=com.myapp com.myapp.MyApp
```

## License

[Apache 2.0](./LICENSE)

## Author

[Rich Dougherty](https://rd.nz)

## Implementation note

A small bytecode hook is added to every method and constructor. This hook tracks when a method has been used, allowing
usage to be recorded. This is very low overhead.

Additionally, once a method has been marked as being used, the bytecode hook can be removed from the method. The
implementation schedules removal of bytecode to happen when the method is called a second time. Removal is done by
rewriting the class bytecode that contains the method to remove the instrumentation bytecode completely, returning the
method to its original bytecode.

Removing the bytecode hook, rather than leaving it in place ensures that methods are quickly returned to their original
bytecode, essentially eliminating any overhead after a few calls. This allows applications to run at full performance
once all methods have been recorded as being used.

Why remove the bytecode hook on the second call rather than the first? Removing it on the second call has some
advantages. One reason is that many methods are only called once, so rewriting their class to remove the bytecode is
wasted work. Another good reason is that when a method is called for the first time, it often calls other methods for
the first time as well. If we delay rewriting class bytecode until more methods have been called, we can avoid rewriting
the  classes too many times, since we can batch changes to multiple methods into each class bytecode rewrite operation.

Apart from bytecode instrumentation overhead, there is still some memory used and some resource usage for logging the
usage. This is not very high, but can certainly be reduced with further optimization work. For example, data structures
can be optimized and operations can be made concurrent to avoid blocking the main program execution.