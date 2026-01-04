package jsonnetjvm.truffle;

import com.oracle.truffle.api.TruffleLanguage.Env;

public class JsonnetContext {
    private final Env env;

    public JsonnetContext(Env env) {
        this.env = env;
    }

    public Env getEnv() {
        return env;
    }
}
