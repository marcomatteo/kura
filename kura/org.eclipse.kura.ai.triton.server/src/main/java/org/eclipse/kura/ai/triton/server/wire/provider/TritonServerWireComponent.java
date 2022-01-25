package org.eclipse.kura.ai.triton.server.wire.provider;

import static java.util.Objects.isNull;

import java.util.Map;

import org.eclipse.kura.ai.inference.engine.InferenceEngineService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TritonServerWireComponent implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(TritonServerWireComponent.class);

    private volatile WireHelperService wireHelperService;
    private WireSupport wireSupport;

    private InferenceEngineService tritonServer;

    public synchronized void setInferenceEngineService(InferenceEngineService tritonServer) {
        if (isNull(this.tritonServer)) {
            this.tritonServer = tritonServer;
        }
    }

    public synchronized void unsetInferenceEngineService(InferenceEngineService tritonServer) {
        if (this.tritonServer == tritonServer) {
            this.tritonServer = null;
            // this.wireRecordStoreOptions = null;
        }
    }

    public void setWireHelperService(final WireHelperService wireHelperService) {
        if (isNull(this.wireHelperService)) {
            this.wireHelperService = wireHelperService;
        }
    }

    public void unsetWireHelperService(final WireHelperService wireHelperService) {
        if (this.wireHelperService == wireHelperService) {
            this.wireHelperService = null;
        }
    }

    protected void activate(final ComponentContext componentContext, final Map<String, Object> properties) {
        logger.info("Activating Triton Server wire component...");
        // this.wireRecordStoreOptions = new H2DbWireRecordStoreOptions(properties);

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

    }

    public void updated(final Map<String, Object> properties) {
        logger.info("Updating Triton Server wire component...");

        // this.wireRecordStoreOptions = new H2DbWireRecordStoreOptions(properties);

    }

    protected void deactivate(final ComponentContext componentContext) {
        logger.info("Deactivating Triton Server wire component...");
        this.tritonServer = null;
        // this.wireRecordStoreOptions = null;
    }

    @Override
    public Object polled(Wire wire) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void consumersConnected(Wire[] wires) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updated(Wire wire, Object value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void producersConnected(Wire[] wires) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onWireReceive(WireEnvelope wireEnvelope) {
        // TODO Auto-generated method stub

    }

}
