package eu.solven.cleanthat.engine.java.refactorer.cases.do_not_format_me;

import java.util.Optional;

public class LiteralsFirstInComparisonsCases {

	public boolean isHardcoded(String input) {
		return input.equals("hardcoded");
	}

	public boolean isPresent(Optional<?> optional) {
		return optional.isPresent();
	}
}
