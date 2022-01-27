package org.eclipse.kura.ai.triton.server.wire.provider;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.ai.inference.engine.InferenceEngineService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import triton.client.InferInput;
import triton.client.InferRequestedOutput;
import triton.client.InferResult;
import triton.client.InferenceException;
import triton.client.InferenceServerClient;
import triton.client.pojo.DataType;

public class TritonServerWireComponent implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(TritonServerWireComponent.class);

    private volatile WireHelperService wireHelperService;
    private WireSupport wireSupport;

    private InferenceEngineService tritonServer;
    private InferenceServerClient tritonClient;
    private TritonServerWireComponentOptions options;

    public synchronized void setInferenceEngineService(InferenceEngineService tritonServer) {
        if (isNull(this.tritonServer)) {
            this.tritonServer = tritonServer;
        }
    }

    public synchronized void unsetInferenceEngineService(InferenceEngineService tritonServer) {
        if (this.tritonServer == tritonServer) {
            this.tritonServer = null;
            this.options = null;
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
        this.options = new TritonServerWireComponentOptions(properties);

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        connect(this.options.getServerAddress() + ":" + this.options.getServerPort());
    }

    public void updated(final Map<String, Object> properties) {
        logger.info("Updating Triton Server wire component...");
        disconnect();
        this.options = new TritonServerWireComponentOptions(properties);
        String ipServerAddress = this.options.getServerAddress() + ":" + this.options.getServerPort();
        connect(ipServerAddress);

    }

    protected void deactivate(final ComponentContext componentContext) {
        logger.info("Deactivating Triton Server wire component...");
        disconnect();
        this.tritonClient = null;
        this.options = null;
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
        requireNonNull(wireEnvelope, "Wire Envelope cannot be null");
        final List<WireRecord> records = wireEnvelope.getRecords();

        if (this.tritonClient == null) {
            logger.warn("Triton client is null"); // change message
            return;
        }

        InferResult result = null;
        List<InferInput> inferInputs = new ArrayList<>();
        List<InferRequestedOutput> inferOutputs = new ArrayList<>();

        for (WireRecord wireRecord : records) {
            inferInputs.add(process(wireRecord));
            inferOutputs.add(new InferRequestedOutput(this.options.getOutputName(), false));
        }

        try {
            result = tritonClient.infer(this.options.getModelName(), inferInputs, inferOutputs);
        } catch (InferenceException e) {
            logger.error("Failed to run inference", e);
        }

        if (!isNull(result)) {
            float[] output = result.getOutputAsFloat("output");
            StringValue value = new StringValue(output[0] + "," + output[1] + "," + output[2]);
            Map<String, TypedValue<?>> properties = new HashMap<String, TypedValue<?>>();
            properties.put("inferenceResult", value);
            WireRecord outputRecord = new WireRecord(properties);
            List<WireRecord> outputRecords = new ArrayList<>();
            outputRecords.add(outputRecord);
            this.wireSupport.emit(outputRecords);
        }
    }

    private InferInput process(WireRecord wireRecord) {
        InferInput inferInput = createInferInput();
        final Map<String, TypedValue<?>> wireRecordProperties = wireRecord.getProperties();
        wireRecordProperties.entrySet().forEach(entry -> {

            final org.eclipse.kura.type.DataType dataType = entry.getValue().getType();
            final Object value = entry.getValue();
            switch (dataType) {
            case BOOLEAN:
                // stmt.setBoolean(i, ((BooleanValue) value).getValue());
                break;
            case FLOAT:

                // stmt.setFloat(i, ((FloatValue) value).getValue());
                break;
            case DOUBLE:
                // stmt.setDouble(i, ((DoubleValue) value).getValue());
                break;
            case INTEGER:
                // stmt.setInt(i, ((IntegerValue) value).getValue());
                break;
            case LONG:
                // stmt.setLong(i, ((LongValue) value).getValue());
                break;
            case BYTE_ARRAY:
                // byte[] byteArrayValue = ((ByteArrayValue) value).getValue();
                // InputStream is = new ByteArrayInputStream(byteArrayValue);
                // stmt.setBlob(i, is);
                break;
            case STRING:
                float[] val = getFloatArray(((StringValue) value).getValue());
                inferInput.setData(val, false);
                // stmt.setString(i, ((StringValue) value).getValue());
                break;
            default:
                break;
            }
        });
        return inferInput;
    }

    private float[] getFloatArray(String value) {
        // not sure about the implementation
        String[] arrayString = value.split(",");
        float[] arrayFloat = new float[arrayString.length];
        for (int i = 0; i < arrayString.length; i++) {
            arrayFloat[i] = Float.parseFloat(arrayString[i]);
        }
        return arrayFloat;
    }

    // private static void irisInference(InferenceServerClient client, float[] irisData, boolean isBinary)
    // throws InferenceException {
    // String modelName = "simple_iris";
    //
    // InferInput input = new InferInput("fc1_input", new long[] { 1, 4 }, DataType.FP32);
    // input.setData(irisData, isBinary);
    //
    // List<InferInput> inputs = Lists.newArrayList(input);
    // List<InferRequestedOutput> outputs = Lists.newArrayList(new InferRequestedOutput("output", isBinary));
    //
    // InferResult result = client.infer(modelName, inputs, outputs);
    //
    // float[] output = result.getOutputAsFloat("output");
    // for (int i = 0; i < output.length; i++) {
    // log(String.valueOf(output[i]));
    // }
    // }

    private void connect(String ipServerAddress) {
        try {
            this.tritonClient = new InferenceServerClient(ipServerAddress, 5000, 5000);
        } catch (IOException e) {
            logger.error("Cannot connect to Nvidia Triton server {}", ipServerAddress, e);
        }
    }

    private void disconnect() {
        try {
            this.tritonClient.close();
            this.tritonClient = null;
        } catch (Exception e) {
            logger.error("Cannot close connection to Nvidia Triton Server");
        }
    }

    private InferInput createInferInput() {
        return new InferInput(this.options.getInputName(), this.options.getInputSize(),
                DataType.valueOf(this.options.getInputType()));
    }
}
