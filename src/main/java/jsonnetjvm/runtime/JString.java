package jsonnetjvm.runtime;

public class JString extends Val {
	private final String value;

	public JString(String value) {
		this.value = value;
	}

	@Override
	public String asString() {
		return value;
	}

	@Override
	public String toJson() {
		return "\"" + value.replace("\"", "\\\"") + "\"";
	}
}
