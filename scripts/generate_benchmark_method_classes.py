import os
import argparse

def generate_method(class_idx, method_idx):
    return f"""
    public static void method{class_idx}_{method_idx}() {{
        // Empty method for benchmarking
    }}
"""

def generate_class(class_idx, num_methods):
    methods = "".join(generate_method(class_idx, i) for i in range(num_methods))
    return f"""
package nz.rd.nonoptest.benchmark.generated;

public class BenchmarkClass{class_idx} {{
{methods}
}}
"""

def generate_method_caller(num_classes, num_methods):
    calls = "".join(
        f"        BenchmarkClass{i}.method{i}_{j}();\n"
        for i in range(num_classes)
        for j in range(num_methods)
    )
    return f"""
package nz.rd.nonoptest.benchmark.generated;

public class MethodCaller {{
    public static void callAllMethods() {{
{calls}
    }}
}}
"""

def write_files(num_classes, num_methods, output_dir):
    os.makedirs(output_dir, exist_ok=True)

    # Generate benchmark classes
    for i in range(num_classes):
        class_code = generate_class(i, num_methods)
        with open(os.path.join(output_dir, f"BenchmarkClass{i}.java"), "w") as f:
            f.write(class_code)

    # Generate MethodCaller
    with open(os.path.join(output_dir, "MethodCaller.java"), "w") as f:
        f.write(generate_method_caller(num_classes, num_methods))

    # Generate Main
#     with open(os.path.join(output_dir, "BenchmarkMain.java"), "w") as f:
#         f.write(generate_main())

def main():
    parser = argparse.ArgumentParser(description="Generate benchmark classes")
    parser.add_argument("--classes", type=int, default=8, help="Number of classes to generate")
    parser.add_argument("--methods", type=int, default=16, help="Number of methods per class")
    parser.add_argument("--output-dir", default="src/test/java/nz/rd/nonoptest/benchmark/generated",
                       help="Output directory for generated classes")
    args = parser.parse_args()

    write_files(args.classes, args.methods, args.output_dir)
    print(f"Generated {args.classes} classes with {args.methods} methods each in {args.output_dir}")

if __name__ == "__main__":
    main()