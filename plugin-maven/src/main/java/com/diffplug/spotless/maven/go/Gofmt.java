package com.diffplug.spotless.maven.go;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.go.GofmtFormatStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;
import org.apache.maven.plugins.annotations.Parameter;

public class Gofmt implements FormatterStepFactory {

    @Parameter
    private String version;

    @Parameter
    private String goExecutablePath;

    @Override
    public FormatterStep newFormatterStep(FormatterStepConfig config) {
        GofmtFormatStep step = GofmtFormatStep.withVersion(version == null ? GofmtFormatStep.defaultVersion() : version);
        if (goExecutablePath != null) {
            step = step.withGoExecutable(goExecutablePath);
        }
        return step.create();
    }
}
