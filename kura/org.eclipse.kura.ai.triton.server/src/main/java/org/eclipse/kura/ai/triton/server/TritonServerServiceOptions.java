package org.eclipse.kura.ai.triton.server;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TritonServerServiceOptions {

    private static final String PROPERTY_ENABLED = "enable";
    private static final String PROPERTY_PORTS = "server.ports";
    private static final String PROPERTY_MODEL_REPOSITORY_PATH = "model.repository.path";
    private static final String PROPERTY_BACKENDS_PATH = "backends.path";
    private static final String PROPERTY_BACKENDS_CONFIG = "backends.config";
    private final Map<String, Object> properties;

    private final int httpPort;
    private final int grpcPort;
    private final int metricsPort;

    public TritonServerServiceOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));

        final Object propertyPorts = this.properties.get(PROPERTY_PORTS);
        if (nonNull(propertyPorts) && propertyPorts instanceof Integer[]) {
            Integer[] ports = (Integer[]) propertyPorts;
            this.httpPort = ports[0];
            this.grpcPort = ports[1];
            this.metricsPort = ports[2];
        } else {
            this.httpPort = 5000;
            this.grpcPort = 5001;
            this.metricsPort = 5002;
        }
    }

    public boolean isEnabled() {
        boolean enabled = false;
        final Object propertyEnabled = this.properties.get(PROPERTY_ENABLED);
        if (nonNull(propertyEnabled) && propertyEnabled instanceof Boolean) {
            enabled = (Boolean) propertyEnabled;
        }
        return enabled;
    }

    public int getHttpPorts() {
        return this.httpPort;
    }

    public int getGrpcPorts() {
        return this.grpcPort;
    }

    public int getMetricsPorts() {
        return this.metricsPort;
    }

    public String getModelRepositoryPath() {
        return getPath(PROPERTY_MODEL_REPOSITORY_PATH);
    }

    public String getBackendsPath() {
        return getPath(PROPERTY_BACKENDS_PATH);
    }

    public List<String> getBackendsConfigs() {
        List<String> backendsConfigs = new ArrayList<>();
        final Object propertyBackendsConfig = this.properties.get(PROPERTY_BACKENDS_CONFIG);
        if (nonNull(propertyBackendsConfig) && propertyBackendsConfig instanceof String) {
            backendsConfigs = Arrays.asList(((String) propertyBackendsConfig).split(";"));
        }
        return backendsConfigs;
    }

    private String getPath(String propertyName) {
        String path = "";
        final Object propertyPath = this.properties.get(propertyName);
        if (nonNull(propertyPath) && propertyPath instanceof String) {
            path = (String) propertyPath;
        }
        return path;
    }
}
