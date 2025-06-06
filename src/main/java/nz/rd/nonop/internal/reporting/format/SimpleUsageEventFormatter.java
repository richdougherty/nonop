// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonop.internal.reporting.format;

import net.bytebuddy.jar.asm.Type;

/**
 * Formats method call events into a simple, human-readable string.
 * Example: com.example.MyClass.myMethod(java.lang.String,int)
 */
public class SimpleUsageEventFormatter implements UsageEventFormatter {

    @Override
    public String formatMethodCalled(long callTimestampMillis, String className, String methodName, String methodDescriptor) {
        StringBuilder sb = new StringBuilder();
        sb.append(className);
        sb.append('.');
        sb.append(methodName); // TODO: Think about formatting for constructors, currently Class.<init>(), could be Class() or new Class()
        sb.append('(');
        // TODO: Think about storing the parsed type in the usage type information so we can reuse
        // TODO: Consider parsing/printing ourselves directly for performance
        Type[] argumentTypes = Type.getArgumentTypes(methodDescriptor);
        for (int i = 0; i < argumentTypes.length; i++) {
            Type type = argumentTypes[i];
            if (i > 0) {
                sb.append(',');
            }
            sb.append(type.getClassName());
        }
        sb.append(')');
        return sb.toString();
    }

}