package nz.rd.nonop.internal.reporting.format;

public interface UsageEventFormatter {
    String formatMethodCalled(long callTimestampMillis, String className, String methodName, String methodDescriptor);
}
