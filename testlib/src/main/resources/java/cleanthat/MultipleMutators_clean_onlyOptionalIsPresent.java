package java.cleanthat;

import java.util.Optional;

public class MultipleMutators_clean_onlyOptionalIsPresent {

	public boolean isHardcoded(String input) {
		return input.equals("hardcoded");
	}

	public boolean isPresent(Optional<?> optional) {
		return optional.isPresent();
	}
}
