package com.diffplug.spotless.maven;

import java.util.concurrent.atomic.AtomicInteger;

public class ImpactedFilesTracker {
	protected final AtomicInteger nbChecked = new AtomicInteger();
	protected final AtomicInteger nbCleaned = new AtomicInteger();

	public void checked() {
		nbChecked.incrementAndGet();
	}

	public int getChecked() {
		return nbChecked.get();
	}

	public void cleaned() {
		nbCleaned.incrementAndGet();
	}

	public int getCleaned() {
		return nbCleaned.get();
	}
}
