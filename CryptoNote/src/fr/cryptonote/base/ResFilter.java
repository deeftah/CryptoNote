package fr.cryptonote.base;

public class ResFilter {
	private static final String[] ignore = 
		{"bower.json", ".gitignore", ".yml", ".md", ".log", "Makefile",
		"index.html", ".js.map", "LICENSE.txt"};

	public boolean filterDir(String dir, String name) {
		if (!AConfig.config().distrib())
			return !(dir.startsWith("/var/bower_components/iron-flex-layout/classes/") ||
					(dir.startsWith("/var/bower_components/") &&
						(name.equals("test/") || name.equals("demo/") || name.equals("/.github/"))));
		else
			return !(dir.startsWith("/var/bower_components/") ||
					dir.startsWith("/var/app_components/") ||
					dir.startsWith("/var/base_components/"));
	}

	public boolean filterFile(String fullpath) {
		for(String x : ignore)
			if (fullpath.endsWith(x)) 
				return false;
		return true;
	}

}
