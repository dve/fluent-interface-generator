package net.vergien.fig.mojo;

import java.util.List;

public class PkgConf {
	private String pkgName;
	private List<String> ignoreMethods;
	private List<String> classNames;
	private String interfacePkgName;
	
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

	public List<String> getIgnoreMethods() {
		return ignoreMethods;
	}

	public void setIgnoreMethods(List<String> ignoreMethods) {
		this.ignoreMethods = ignoreMethods;
	}

	public String getInterfacePkgName() {
		return interfacePkgName;
	}

	public void setInterfacePkgName(String interfacePkgName) {
		this.interfacePkgName = interfacePkgName;
	}
	
}
