package com.diffplug.gradle.spotless;

import java.io.File;
import java.util.Objects;
import java.util.function.Predicate;

final class FilterByFileFormatterStep implements FormatterStep {
    private final FormatterStep delegateStep;
    private final Predicate<File> filter;

    FilterByFileFormatterStep(FormatterStep delegateStep, Predicate<File> filter) {
        this.delegateStep = delegateStep;
        this.filter = filter;
    }

    @Override
    public String getName() {
        return delegateStep.getName();
    }

    @Override
    public String format(String raw, File file) throws Throwable {
        if (filter.test(file)) {
            return delegateStep.format(raw, file);
        } else {
            return raw;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilterByFileFormatterStep that = (FilterByFileFormatterStep) o;
        return Objects.equals(delegateStep, that.delegateStep) &&
            Objects.equals(filter, that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegateStep, filter);
    }

    private static final long serialVersionUID = 1L;
}
