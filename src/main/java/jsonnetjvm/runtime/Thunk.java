package jsonnetjvm.runtime;

import java.util.function.Supplier;

public class Thunk implements Supplier<Val> {
	private Supplier<Val> supplier;
	private Val value;

	public Thunk(Supplier<Val> supplier) {
		this.supplier = supplier;
	}

	@Override
	public Val get() {
		if (supplier != null) {
			value = supplier.get();
			supplier = null;
		}
		return value;
	}
}
