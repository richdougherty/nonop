package nz.rd.nonop.internal.transformer;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import nz.rd.nonop.config.scan.ScanMatcher;
import nz.rd.nonop.internal.util.NonopLogger;

import java.util.List;

class NameBasedScanRuleMatcher implements ElementMatcher<TypeDescription> {
    private final List<ScanMatcher> matchers; // Changed from rules
    private final NonopLogger logger;

    public NameBasedScanRuleMatcher(List<ScanMatcher> matchers, NonopLogger logger) {
        this.matchers = matchers;
        this.logger = logger;
    }

    @Override
    public boolean matches(TypeDescription target) {
        String className = target.getActualName();
        return matchesClassName(className);
    }

    public boolean matchesClassName(String className) {
        if (matchers.isEmpty()) {
            return false;
        }

        // Iterate through all matchers. The last one that provides a non-null decision wins.
        boolean lastWasInclude = false;
        for (ScanMatcher matcher : matchers) {
            Boolean ruleEval = matcher.eval(className);
            if (ruleEval != null) {
                if (className.startsWith("nz.rd.nonoptest.")) {
                    logger.debug("[Matcher] Rule " + matcher.getPatternString() + " evaluated for " + className + " -> " + ruleEval);
                }
                return ruleEval;
            }
            lastWasInclude = matcher.isEffectivelyInclude();
        }

        // If the last matcher was a 'not' matcher then the user was just excluding some classes, so they probably
        // want to match anything they didn't exclude. Conversely, if the last matcher was not a 'not' matcher then they
        // were just including some classes, so they probably want to skip anything they didn't include.

        return !lastWasInclude;

    }
}
