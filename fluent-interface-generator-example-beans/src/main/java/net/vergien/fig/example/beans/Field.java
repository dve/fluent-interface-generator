package net.vergien.fig.example.beans;

public class Field extends AbstractBean implements IField<String> {
	private String value;

	@Override
	public void setValue(String value) {
		this.value = value;

	}

	public void setO(Integer o) {
		super.setO(o);
	};
}
