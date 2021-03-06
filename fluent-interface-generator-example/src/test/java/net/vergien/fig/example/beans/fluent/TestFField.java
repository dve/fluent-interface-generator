package net.vergien.fig.example.beans.fluent;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;

public class TestFField extends TestCase {
	private MField fieldUnderTest;

	@Before
	public void setUp() {
		fieldUnderTest = new MField();
	}

	@Test
	public void testWithValue() {
		fieldUnderTest.withValue("Test");
		
		assertThat(fieldUnderTest.getValue(), is("Test"));
	}
	@Test
	public void testWithStyleName() {
		fieldUnderTest.withStyleName("Test").withStyleName("Test2");

		assertThat(fieldUnderTest.getStyleNames().size(), is(2));
		assertThat(fieldUnderTest.getStyleNames().contains("Test"), is(true));
		assertThat(fieldUnderTest.getStyleNames().contains("Test2"), is(true));
	}

	@Test
	public void testWithStyleName_varags() {
		fieldUnderTest.withStyleName("Test", "Test2");
		
		assertThat(fieldUnderTest.getStyleNames().size(), is(2));
		assertThat(fieldUnderTest.getStyleNames().contains("Test"), is(true));
		assertThat(fieldUnderTest.getStyleNames().contains("Test2"), is(true));
	}
	
	@Test
	public void testWithFullSize(){
		fieldUnderTest.withFullSize();
		
		assertThat(fieldUnderTest.getSize(), is(100));
	}
}
