/*
 * Copyright 2016 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.npm;

import com.diffplug.spotless.*;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.requireNonNull;

abstract class NpmFormatterStepStateBase implements Serializable {

    private static final long serialVersionUID = -5849375492831208496L;

    private final JarState jarState;

    @SuppressWarnings("unused")
    private final FileSignature nodeModulesSignature;

    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    public final transient File nodeModulesDir;

    private final transient File npmExecutable;

    private final NpmConfig npmConfig;

    private final String stepName;

    protected NpmFormatterStepStateBase(String stepName, Provisioner provisioner, NpmConfig npmConfig, File buildDir, @Nullable File npm) throws IOException {
        this.stepName = requireNonNull(stepName);
        this.npmConfig = requireNonNull(npmConfig);
        this.jarState = JarState.from(j2v8MavenCoordinate(), requireNonNull(provisioner));
        this.npmExecutable = resolveNpm(npm);

        this.nodeModulesDir = prepareNodeServer(buildDir);
        this.nodeModulesSignature = FileSignature.signAsList(this.nodeModulesDir);
    }

    private File prepareNodeServer(File buildDir) throws IOException {
        File targetDir = new File(buildDir, "spotless-node-modules-" + stepName);
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                throw new IOException("cannot create temp dir for node modules at " + targetDir);
            }
        }
        writeContentToFile(targetDir, "package.json", this.npmConfig.getPackageJsonContent());
        writeContentToFile(targetDir, "serve.js", this.npmConfig.getServeScriptContent());
        runNpmInstall(targetDir);
        return targetDir;
    }

    private void writeContentToFile(File targetDir, String s, String packageJsonContent) throws IOException {
        File packageJsonFile = new File(targetDir, s);
        Files.write(packageJsonFile.toPath(), packageJsonContent.getBytes(StandardCharsets.UTF_8));
    }

    private void runNpmInstall(File npmProjectDir) throws IOException {
        Process npmInstall = new ProcessBuilder()
                .inheritIO()
                .directory(npmProjectDir)
                .command(this.npmExecutable.getAbsolutePath(), "install", "--no-audit", "--no-package-lock")
                .start();
        try {
            if (npmInstall.waitFor() != 0) {
                throw new IOException("Creating npm modules failed with exit code: " + npmInstall.exitValue());
            }
        } catch (InterruptedException e) {
            throw new IOException("Running npm install was interrupted.", e);
        }
    }

    protected ServerProcessInfo npmRunServer() throws ServerStartException {
        try {
            Process server = new ProcessBuilder()
                    .inheritIO()
//                    .redirectError(new File("/Users/simschla/tmp/npmerror.log"))
//                    .redirectOutput(new File("/Users/simschla/tmp/npmout.log"))
                    .directory(this.nodeModulesDir)
                    .command(this.npmExecutable.getAbsolutePath(), "start")
                    .start();

            File serverPortFile = new File(this.nodeModulesDir, "server.port");
            final long startedAt = System.currentTimeMillis();
            while (!serverPortFile.exists() || !serverPortFile.canRead()) {
                // wait for at most 10 seconds
                if ((System.currentTimeMillis() - startedAt) > (10 * 1000L)) {
                    // forcibly end the server process
                    try {
                        server.destroyForcibly();
                    } catch (Throwable t) {
                        // log this?
                    }
                    throw new TimeoutException("The server did not startup in the requested time frame of 10 seconds.");
                }
            }
            // readPort from file

            // read the server.port file for resulting port
            String serverPort = readFile(serverPortFile).trim();
            return new ServerProcessInfo(server, serverPort, serverPortFile);
        } catch (IOException | TimeoutException e) {
            throw new ServerStartException(e);
        }
    }

    private static File resolveNpm(@Nullable File npm) {
        return Optional.ofNullable(npm)
                .orElseGet(() -> NpmExecutableResolver.tryFind()
                        .orElseThrow(() -> new IllegalStateException("cannot automatically determine npm executable and none was specifically supplied!")));
    }

    protected NodeJSWrapper nodeJSWrapper() {
        return new NodeJSWrapper(this.jarState.getClassLoader());
    }

    protected File nodeModulePath() {
        return new File(new File(this.nodeModulesDir, "node_modules"), this.npmConfig.getNpmModule());
    }

    static String j2v8MavenCoordinate() {
        return "com.eclipsesource.j2v8:j2v8_" + PlatformInfo.normalizedOSName() + "_" + PlatformInfo.normalizedArchName() + ":4.6.0";
    }

    protected static String readFileFromClasspath(Class<?> clazz, String name) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = clazz.getResourceAsStream(name)) {
            byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, numRead);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw ThrowingEx.asRuntime(e);
        }
    }

    protected static String readFile(File file) {
        try {
            return String.join("\n", Files.readAllLines(file.toPath()));
        } catch (IOException e) {
            throw ThrowingEx.asRuntime(e);
        }
    }

    protected static String replaceDevDependencies(String template, Map<String, String> devDependencies) {
        StringBuilder builder = new StringBuilder();
        Iterator<Map.Entry<String, String>> entryIter = devDependencies.entrySet().iterator();
        while (entryIter.hasNext()) {
            Map.Entry<String, String> entry = entryIter.next();
            builder.append("\t\t\"");
            builder.append(entry.getKey());
            builder.append("\": \"");
            builder.append(entry.getValue());
            builder.append("\"");
            if (entryIter.hasNext()) {
                builder.append(",\n");
            }
        }
        return replacePlaceholders(template, Collections.singletonMap("devDependencies", builder.toString()));
    }

    private static String replacePlaceholders(String template, Map<String, String> replacements) {
        String result = template;
        for (Entry<String, String> entry : replacements.entrySet()) {
            result = result.replaceAll("\\Q${" + entry.getKey() + "}\\E", entry.getValue());
        }
        return result;
    }

    public abstract FormatterFunc createFormatterFunc();

    protected class ServerProcessInfo implements AutoCloseable {
        private final Process server;
        private final String serverPort;
        private final File serverPortFile;

        public ServerProcessInfo(Process server, String serverPort, File serverPortFile) {
            this.server = server;
            this.serverPort = serverPort;
            this.serverPortFile = serverPortFile;
        }

        public String getBaseUrl() {
            return "http://127.0.0.1:" + this.serverPort;
        }

        @Override
        public void close() throws Exception {
            if (serverPortFile.exists()) {
                serverPortFile.delete();
            }
            if (this.server.isAlive()) {
                this.server.destroy();
            }
        }
    }

    protected class ServerStartException extends RuntimeException {
        public ServerStartException(Throwable cause) {
            super(cause);
        }
    }
}
