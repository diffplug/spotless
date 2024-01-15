package com.diffplug.spotless.go;

import com.diffplug.spotless.ForeignExe;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ProcessRunner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Note: gofmt doesn't have a version flag, because it's part of standard Go distribution.
 * So `go` executable can be used to determine base path and version, and path to gofmt can be built from it.
 */
public class GofmtFormatStep {
    public static String name() {
        return "gofmt";
    }

    public static String defaultVersion() {
        return "1.20.0";
    }

    private final String version;
    private final @Nullable String pathToExe;

    private GofmtFormatStep(String version, String pathToExe) {
        this.version = version;
        this.pathToExe = pathToExe;
    }

    public static GofmtFormatStep withVersion(String version) {
        return new GofmtFormatStep(version, null);
    }

    public GofmtFormatStep withGoExecutable(String pathToExe) {
        return new GofmtFormatStep(version, pathToExe);
    }

    public FormatterStep create() {
        return FormatterStep.createLazy(name(), this::createState, GofmtFormatStep.State::toFunc);
    }

    private State createState() throws IOException, InterruptedException {
        String howToInstall = "gofmt is a part of standard go distribution. If spotless can't discover it automatically, " +
                "you can point Spotless to the go binary with {@code pathToExe('/path/to/go')}";
        final ForeignExe exe = ForeignExe.nameAndVersion("go", version)
                .pathToExe(pathToExe)
                .versionFlag("version")
                .fixCantFind(howToInstall)
                .fixWrongVersion(
                        "You can tell Spotless to use the version you already have with {@code gofmt('{versionFound}')}" +
                                "or you can install the currently specified Go version, {version}.\n" + howToInstall);
        return new State(this, exe);
    }

    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    static class State implements Serializable {
        private static final long serialVersionUID = -1825662355363926318L;
        // used for up-to-date checks and caching
        final String version;
        final transient ForeignExe exe;

        public State(GofmtFormatStep step, ForeignExe goExecutable) {
            this.version = step.version;
            this.exe = Objects.requireNonNull(goExecutable);
        }

        String format(ProcessRunner runner, String input, File file) throws IOException, InterruptedException {
            final List<String> processArgs = new ArrayList<>();
            String pathToGoBinary = exe.confirmVersionAndGetAbsolutePath();
            String pathToGoFmt = Path.of(pathToGoBinary).getParent().resolve("gofmt").toString();
            processArgs.add(pathToGoFmt);
            return runner.exec(input.getBytes(StandardCharsets.UTF_8), processArgs).assertExitZero(StandardCharsets.UTF_8);
        }

        FormatterFunc.Closeable toFunc() {
            ProcessRunner runner = new ProcessRunner();
            return FormatterFunc.Closeable.of(runner, this::format);
        }
    }
}
