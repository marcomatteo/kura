package org.eclipse.kura.wire.internal.ai.triton.client;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TritonServerClientOptions {

    private static final String PROPERTY_SERVER_ADDRESS = "server.address";
    private static final String PROPERTY_SERVER_PORT = "server.port";
    private static final String PROPERTY_MODEL_NAME = "model.name";
    private static final String PROPERTY_INPUT_NAME = "input.name";
    private static final String PROPERTY_INPUT_SIZE = "input.size";
    private static final String PROPERTY_INPUT_TYPE = "input.type";
    private static final String PROPERTY_OUTPUT_NAME = "output.name";
    private static final String PROPERTY_OUTPUT_TYPE = "output.type";
    private static final String EMIT_ON_EMPTY_RESULT = "emit.on.empty.result";
    private final Map<String, Object> properties;

    public TritonServerClientOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
    }

    public String getServerAddress() {
        return getStringValue(PROPERTY_SERVER_ADDRESS);
    }

    public Integer getServerPort() {
        Integer value = 0;
        final Object propertyPath = this.properties.get(PROPERTY_SERVER_PORT);
        if (nonNull(propertyPath) && propertyPath instanceof Integer) {
            value = (Integer) propertyPath;
        }
        return value;
    }

    public String getModelName() {
        return getStringValue(PROPERTY_MODEL_NAME);
    }

    public String getInputName() {
        return getStringValue(PROPERTY_INPUT_NAME);
    }

    public long[] getInputSize() {
        return getLongArray(PROPERTY_INPUT_SIZE);
    }

    public String getInputType() {
        return getStringValue(PROPERTY_INPUT_TYPE);
    }

    public String getOutputName() {
        return getStringValue(PROPERTY_OUTPUT_NAME);
    }

    public String getOutputType() {
        return getStringValue(PROPERTY_OUTPUT_TYPE);
    }

    public boolean emitOnEmptyResult() {
        boolean result = true;
        final Object emitOnEmptyResult = this.properties.get(EMIT_ON_EMPTY_RESULT);
        if (nonNull(emitOnEmptyResult) && emitOnEmptyResult instanceof Boolean) {
            result = (Boolean) emitOnEmptyResult;
        }
        return result;
    }

    private String getStringValue(String propertyName) {
        String value = "";
        final Object propertyPath = this.properties.get(propertyName);
        if (nonNull(propertyPath) && propertyPath instanceof String) {
            value = (String) propertyPath;
        }
        return value;
    }

    private long[] getLongArray(String propertyName) {
        // not sure about the implementation
        String[] arrayString = getStringValue(propertyName).split(",");
        long[] arrayLong = new long[arrayString.length];
        for (int i = 0; i < arrayString.length; i++) {
            arrayLong[i] = Long.parseLong(arrayString[i]);
        }
        return arrayLong;
    }
}
