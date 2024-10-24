/*
 * Copyright 2021-2024 DiffPlug
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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.Internal;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.Unhandled;
import com.diffplug.spotless.Lint;
import com.diffplug.spotless.Provisioner;

/**
 * Allows the check and apply tasks to coordinate
 * with each other (and the source task) to reduce
 * duplicated work (e.g. no need for check to run if
 * apply already did).
 */
public abstract class SpotlessTaskService implements BuildService<BuildServiceParameters.None>, AutoCloseable, OperationCompletionListener {
	private final Map<String, SpotlessApply> apply = Collections.synchronizedMap(new HashMap<>());
	private final Map<String, SpotlessTask> source = Collections.synchronizedMap(new HashMap<>());
	private final Map<String, Provisioner> provisioner = Collections.synchronizedMap(new HashMap<>());

	@Nullable
	GradleProvisioner.DedupingProvisioner predeclaredProvisioner;

	Provisioner provisionerFor(SpotlessExtension spotless) {
		if (spotless instanceof SpotlessExtensionPredeclare) {
			return predeclaredProvisioner;
		} else {
			if (predeclaredProvisioner != null) {
				return predeclaredProvisioner.cachedOnly;
			} else {
				return provisioner.computeIfAbsent(spotless.project.getPath(), unused -> new GradleProvisioner.DedupingProvisioner(GradleProvisioner.forProject(spotless.project)));
			}
		}
	}

	void registerSourceAlreadyRan(SpotlessTask task) {
		source.put(task.getPath(), task);
	}

	void registerApplyAlreadyRan(SpotlessApply task) {
		apply.put(task.sourceTaskPath(), task);
	}

	// <GitRatchet>
	private final GitRatchetGradle ratchet = new GitRatchetGradle();

	GitRatchetGradle getRatchet() {
		return ratchet;
	}

	@Override
	public void onFinish(FinishEvent var1) {
		// NOOP
	}

	@Override
	public void close() throws Exception {
		ratchet.close();
	}
	// </GitRatchet>

	static String INDEPENDENT_HELPER = "Helper";

	static void usesServiceTolerateTestFailure(DefaultTask task, Provider<SpotlessTaskService> serviceProvider) {
		try {
			task.usesService(serviceProvider);
		} catch (ClassCastException e) {
			// this happens only in our test mocking, e.g. DiffMessageFormatterTest
			// https://github.com/diffplug/spotless/pull/1570/commits/c45e1f2322c78f272689feb35753bbc633422bfa
			// it's fine to swallow these exceptions
		}
	}

	static abstract class ClientTask extends DefaultTask {
		@Internal
		abstract Property<File> getSpotlessCleanDirectory();

		@Internal
		abstract Property<File> getSpotlessLintsDirectory();

		@Internal
		abstract Property<SpotlessTaskService> getTaskService();

		@Internal
		abstract DirectoryProperty getProjectDir();

		@Inject
		protected abstract ObjectFactory getConfigCacheWorkaround();

		void init(SpotlessTaskImpl impl) {
			usesServiceTolerateTestFailure(this, impl.getTaskServiceProvider());
			getSpotlessCleanDirectory().set(impl.getCleanDirectory());
			getSpotlessLintsDirectory().set(impl.getLintsDirectory());
			getTaskService().set(impl.getTaskService());
			getProjectDir().set(impl.getProjectDir());
		}

		String sourceTaskPath() {
			String path = getPath();
			if (this instanceof SpotlessApply) {
				if (path.endsWith(SpotlessExtension.APPLY)) {
					return path.substring(0, path.length() - SpotlessExtension.APPLY.length());
				} else {
					return path + INDEPENDENT_HELPER;
				}
			} else if (this instanceof SpotlessCheck) {
				Preconditions.checkArgument(path.endsWith(SpotlessExtension.CHECK));
				return path.substring(0, path.length() - SpotlessExtension.CHECK.length());
			} else {
				throw Unhandled.classException(this);
			}
		}

		private SpotlessTaskService service() {
			return getTaskService().get();
		}

		protected boolean sourceDidWork() {
			SpotlessTask sourceTask = service().source.get(sourceTaskPath());
			if (sourceTask != null) {
				return sourceTask.getDidWork();
			} else {
				return false;
			}
		}

		protected boolean applyHasRun() {
			return service().apply.containsKey(sourceTaskPath());
		}

		protected String allLintsErrorMsgDetailed(ConfigurableFileTree lintsFiles, boolean detailed) {
			AtomicInteger total = new AtomicInteger(0);
			TreeMap<String, LinkedHashMap<String, List<Lint>>> allLints = new TreeMap<>();
			lintsFiles.visit(new FileVisitor() {
				@Override
				public void visitDir(FileVisitDetails fileVisitDetails) {

				}

				@Override
				public void visitFile(FileVisitDetails fileVisitDetails) {
					String path = fileVisitDetails.getPath();
					getLogger().debug("Reading lints for " + path);
					LinkedHashMap<String, List<Lint>> lints = SerializableMisc.fromFile(LinkedHashMap.class, fileVisitDetails.getFile());
					allLints.put(path, lints);
					lints.values().forEach(list -> total.addAndGet(list.size()));
				}
			});
			StringBuilder builder = new StringBuilder();
			builder.append("There were " + total.get() + " lint error(s), they must be fixed or suppressed.\n");
			for (Map.Entry<String, LinkedHashMap<String, List<Lint>>> lintsPerFile : allLints.entrySet()) {
				for (Map.Entry<String, List<Lint>> stepLints : lintsPerFile.getValue().entrySet()) {
					String stepName = stepLints.getKey();
					for (Lint lint : stepLints.getValue()) {
						builder.append(lintsPerFile.getKey());
						builder.append(":");
						boolean oneLine = !detailed;
						lint.addWarningMessageTo(builder, stepName, oneLine);
						builder.append("\n");
					}
				}
			}
			builder.append("Resolve these lints or suppress with `suppressLintsFor`");
			return builder.toString();
		}
	}
}
