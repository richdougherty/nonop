# nonop

Prototype of an extremely low overhead, simple to use Java dead code detector. `nonop` is attached to a Java processes
as an agent and automatically detects and prints out method usage.

Methods are instrumented automatically, but *all instrumentation is automatically removed when no longer needed*,
which means zero overhead after each method has been called once.

Additionally, effort is taken to make even the instrumented code path as fast as possible.

## Usage

Supports Java 8+.

The `nonop` agent runs as a standard JVM agent. The prototype prints method usage to stdout.

```
java -javaagent:nonop-<version>.jar MyApp
[nonop] INFO  METHOD_CALLED: MyApp main([Ljava/lang/String;)V
[nonop] INFO  METHOD_CALLED: ....
[nonop] INFO  METHOD_CALLED: ....
[nonop] INFO  METHOD_CALLED: ....
[nonop] INFO  METHOD_CALLED: ....
[nonop] INFO  METHOD_CALLED: ....
...
```

## License

[Apache 2.0](./LICENSE)

## Author

[Rich Dougherty](https://rd.nz)