package java.cleanthat;

import java.util.Optional;

public class MultipleMutators_clean_onlyLiteralsFirst {

	public boolean isHardcoded(String input) {
		return "hardcoded".equals(input);
	}

	public boolean isPresent(Optional<?> optional) {
		return !optional.isEmpty();
	}
}
