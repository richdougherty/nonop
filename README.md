# nonop

Prototype of an extremely low overhead, simple to use Java dead code detector. `nonop` is attached to a Java processes
as an agent and automatically detects and prints out method usage.

Methods are instrumented automatically, but *all instrumentation is automatically removed when no longer needed*,
which means zero overhead after each method has been called.[†](#footnotes)

Additionally, effort is taken to make even the instrumented code path as fast as possible.

## Usage

Supports Java 8+.

The `nonop` agent runs as a standard JVM agent. By default all your app's code is instrumented and usage information is
printed to stdout.

```
java -javaagent:nonop-<version>.jar com.myapp.MyApp
```

The `nonop.out` property can direct usage logging to a file instead of stdout.
```
java -javaagent:nonop-<version>.jar -Dnonop.out=nonop.log com.myapp.MyApp
```

The `nonop.scan` property can be set to restrict the classes that nonop instruments.
```
java -javaagent:nonop-<version>.jar -Dnonop.scan=com.myapp com.myapp.MyApp
```

## License

[Apache 2.0](./LICENSE)

## Author

[Rich Dougherty](https://rd.nz)

## Footnotes

† Note the current algorithm removes a method's instrumentation after the _second_ call to a method. Removing on the
first call  is too  aggressive and results in too-frequent full class reinstrumentation vs the small overhead of running
the method instrumentation twice.