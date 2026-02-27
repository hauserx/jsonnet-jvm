// CheckStyle: start generated
package com.databricks.jsonnetjvm.truffle;

import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import com.oracle.truffle.api.TruffleLanguage.Registration;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.provider.TruffleLanguageProvider;
import java.util.Collection;
import java.util.List;

@GeneratedBy(JsonnetLanguage.class)
@Registration(characterMimeTypes = "application/x-jsonnet", contextPolicy = ContextPolicy.REUSE, id = "jsonnet", name = "Jsonnet", version = "0.1")
public final class JsonnetLanguageProvider extends TruffleLanguageProvider {

    @Override
    protected String getLanguageClassName() {
        return "com.databricks.jsonnetjvm.truffle.JsonnetLanguage";
    }

    @Override
    protected Object create() {
        return new JsonnetLanguage();
    }

    @Override
    protected Collection<String> getServicesClassNames() {
        return List.of();
    }

    @Override
    protected List<?> createFileTypeDetectors() {
        return List.of();
    }

    @Override
    protected List<String> getInternalResourceIds() {
        return List.of();
    }

    @Override
    protected Object createInternalResource(String resourceId) {
        throw new IllegalArgumentException(String.format("Unsupported internal resource id %s, supported ids are ", resourceId));
    }

}
