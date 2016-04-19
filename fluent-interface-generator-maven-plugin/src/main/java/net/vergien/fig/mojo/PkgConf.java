package net.vergien.fig.mojo;

import java.util.List;

public class PkgConf {
	private String pkgName;
	private List<String> classNames;

	public String getPkgName() {
		return pkgName;
	}

	public void setPkgName(String pkgName) {
		this.pkgName = pkgName;
	}

	public List<String> getClassNames() {
		return classNames;
	}

	public void setClassNames(List<String> classNames) {
		this.classNames = classNames;
	}
}
