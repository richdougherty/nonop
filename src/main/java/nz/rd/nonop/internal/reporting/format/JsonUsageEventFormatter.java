package nz.rd.nonop.internal.reporting.format;

public class JsonUsageEventFormatter implements UsageEventFormatter {

    // TODO: Consider a StringBuilder interface if we want to save allocations at some point
    public String formatMethodCalled(long callTimestampMillis, String className, String methodName, String methodDescriptor) {
        return "{" +
                "\"timestamp\":" + callTimestampMillis + "," +
                "\"type\":\"method-called\"," +
                "\"class\":\"" + className + "\"," +
                "\"method\":\"" + methodName + "\"," +
                "\"descriptor\":\"" + methodDescriptor + "\"" +
                "}";
    }

}
