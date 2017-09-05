package net.yadaframework.web;

public class YadaJsonView {

	// Returns all attributes that are fetched in a single query
	public class WithEagerAttributes {};

	// Returns only the current localized value, not the whole map
	public class WithLocalizedValue extends WithEagerAttributes {};
	
	// Returns also maps of localized attributes
	public class WithLocalizedStrings extends WithLocalizedValue {};
	
	// Returns lazy attributes
	public class WithLazyAttributes extends WithLocalizedStrings {};
	
}
