package net.vergien.fig.example.beans;

import java.util.ArrayList;
import java.util.List;

public class Field extends AbstractBean implements IField<String> {
	private String value;
	private List<String> styleNames = new ArrayList<>();

	@Override
	public void setValue(String value) {
		this.value = value;

	}

	public void setStyleName(String styleName) {
		styleNames.clear();
		styleNames.add(styleName);
	}

	public void addStyleName(String styleName) {
		styleNames.add(styleName);
	}

	public void addStyleName(String... styleNames) {
		for (String styleName : styleNames) {
			addStyleName(styleName);
		}
	}

	public void setO(Integer o) {
		super.setO(o);
	}

	public String getValue() {
		return value;
	}

	public List<String> getStyleNames() {
		return styleNames;
	};
}
