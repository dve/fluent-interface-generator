package net.vergien.fig.example.beans;

public class AbstractField<T, A> {
	private T value;
	private A otherValue;

	public T getValue() {
		return value;
	}

	public void setValues(T value, A otherValue) {
		this.value = value;
		this.otherValue = otherValue;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public void setValueAt(int pos, T value) {
		this.value = value;
	}

	public A getOtherValue() {
		return otherValue;
	}

}
