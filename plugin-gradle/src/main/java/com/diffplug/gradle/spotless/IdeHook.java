/*
 * Copyright 2016-2021 DiffPlug
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
import java.io.IOException;

import com.diffplug.common.io.ByteStreams;
import com.diffplug.spotless.PaddedCell;

class IdeHook extends FileApplier {
	final static String PROPERTY = "spotlessIdeHook";
	final static String USE_STD_IN = "spotlessIdeHookUseStdIn";
	final static String USE_STD_OUT = "spotlessIdeHookUseStdOut";

	static void performHook(SpotlessTaskImpl spotlessTask) {
		String path = (String) spotlessTask.getProject().property(PROPERTY);
		File file = new File(path);
		if (!file.isAbsolute()) {
			System.err.println("Argument passed to " + PROPERTY + " must be an absolute path");
			return;
		}
		new IdeHook().applyFile(spotlessTask, file);
	}

	@Override
	protected byte[] read(SpotlessTaskImpl spotlessTask, File file) throws IOException {
		if (spotlessTask.getProject().hasProperty(USE_STD_IN)) {
			return ByteStreams.toByteArray(System.in);
		} else {
			return super.read(spotlessTask, file);
		}
	}

	@Override
	protected void writeCanonical(SpotlessTaskImpl spotlessTask, PaddedCell.DirtyState dirty, File file) throws IOException {
		if (spotlessTask.getProject().hasProperty(USE_STD_OUT)) {
			dirty.writeCanonicalTo(System.out);
		} else {
			super.writeCanonical(spotlessTask, dirty, file);
		}
	}

	@Override
	protected void onResult(SpotlessApplyResult result) {
		switch (result) {
		case CLEAN:
			System.err.println("IS CLEAN");
			break;
		case DID_NOT_CONVERGE:
			System.err.println("DID NOT CONVERGE");
			System.err.println("Run 'spotlessDiagnose' for details https://github.com/diffplug/spotless/blob/main/PADDEDCELL.md");
			break;
		case DIRTY:
			System.err.println("IS DIRTY");
			break;
		default:
		}
	}

	@Override
	protected void onException(Exception e) {
		e.printStackTrace(System.err);
	}

	@Override
	protected void onFinished() {
		System.err.close();
		System.out.close();
	}
}
