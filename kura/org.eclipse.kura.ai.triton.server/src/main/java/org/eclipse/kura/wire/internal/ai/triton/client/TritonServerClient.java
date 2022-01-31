package org.eclipse.kura.wire.internal.ai.triton.client;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.ByteArrayValue;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
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

import com.google.gson.Gson;

import triton.client.InferInput;
import triton.client.InferRequestedOutput;
import triton.client.InferResult;
import triton.client.InferenceException;
import triton.client.InferenceServerClient;
import triton.client.pojo.DataType;

public class TritonServerClient implements WireEmitter, WireReceiver, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(TritonServerClient.class);

    private volatile WireHelperService wireHelperService;
    private WireSupport wireSupport;

    private InferenceServerClient tritonClient;
    private TritonServerClientOptions options;

    private Gson gson = new Gson();

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
        this.options = new TritonServerClientOptions(properties);

        this.wireSupport = this.wireHelperService.newWireSupport(this,
                (ServiceReference<WireComponent>) componentContext.getServiceReference());

        startInferenceServerClient(this.options.getServerAddress() + ":" + this.options.getServerPort());
        logger.info("Activating Triton Server wire component...Done");
    }

    public void updated(final Map<String, Object> properties) {
        logger.info("Updating Triton Server wire component...");
        closeInferenceServerClient();
        this.options = new TritonServerClientOptions(properties);
        String ipServerAddress = this.options.getServerAddress() + ":" + this.options.getServerPort();
        startInferenceServerClient(ipServerAddress);
        logger.info("Updating Triton Server wire component...Done");
    }

    protected void deactivate(final ComponentContext componentContext) {
        logger.info("Deactivating Triton Server wire component...");
        closeInferenceServerClient();
        this.tritonClient = null;
        this.options = null;
        logger.info("Updating Triton Server wire component...Done");
    }

    /** {@inheritDoc} */
    @Override
    public Object polled(Wire wire) {
        return this.wireSupport.polled(wire);
    }

    /** {@inheritDoc} */
    @Override
    public void consumersConnected(Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void updated(Wire wire, Object value) {
        this.wireSupport.updated(wire, value);
    }

    /** {@inheritDoc} */
    @Override
    public void producersConnected(Wire[] wires) {
        this.wireSupport.producersConnected(wires);
    }

    /** {@inheritDoc} */
    @Override
    public void onWireReceive(WireEnvelope wireEnvelope) {
        requireNonNull(wireEnvelope, "Wire Envelope cannot be null");
        final List<WireRecord> records = wireEnvelope.getRecords();

        if (this.tritonClient == null) {
            logger.warn("Cannot process input data: Nvidia Triton client is null");
            return;
        }

        List<InferInput> inferInputs = new ArrayList<>();
        List<InferRequestedOutput> inferOutputs = new ArrayList<>();

        records.forEach(wireRecord -> getInferInput(wireRecord).ifPresent(inferInput -> {
            inferInputs.add(inferInput);
            inferOutputs.add(new InferRequestedOutput(this.options.getOutputName(), true));
        }));

        List<WireRecord> result;
        if (!inferInputs.isEmpty()) {
            InferResult inferResult = runInference(inferInputs, inferOutputs);
            if (nonNull(inferResult)) {
                result = Collections.unmodifiableList(getOutputRecords(inferResult));
            } else {
                result = Collections.unmodifiableList(new ArrayList<WireRecord>());
            }
        } else {
            result = Collections.unmodifiableList(new ArrayList<WireRecord>());
        }

        if (!result.isEmpty() || this.options.emitOnEmptyResult()) {
            this.wireSupport.emit(result);
        }
    }

    private Optional<InferInput> getInferInput(WireRecord wireRecord) {
        Optional<InferInput> inferInputOptional = Optional.empty();
        final Map<String, TypedValue<?>> wireRecordProperties = wireRecord.getProperties();
        StringValue inputData = (StringValue) wireRecordProperties.get(this.options.getInputName());
        if (nonNull(inputData)) {
            InferInput inferInput = createInferInput();

            final org.eclipse.kura.type.DataType dataType = inputData.getType();
            final DataType modelDataType = DataType.valueOf(this.options.getInputType());
            if (!dataType.equals(org.eclipse.kura.type.DataType.STRING)) {
                logger.warn("Only Json String input are supported");
                return inferInputOptional;
            }

            switch (modelDataType) {
            case BOOL:
                boolean[] booleanValue = this.gson.fromJson(inputData.getValue(), boolean[].class);
                inferInput.setData(booleanValue, true);
                break;
            case UINT8:
            case INT8:
                byte[] byteValue = this.gson.fromJson(inputData.getValue(), byte[].class);
                inferInput.setData(byteValue, true);
                break;
            case UINT16:
            case INT16:
                int[] value = this.gson.fromJson(inputData.getValue(), int[].class);
                inferInput.setData(convertToShort(value), true);
                break;
            case UINT32:
            case INT32:
                int[] integerValue = this.gson.fromJson(inputData.getValue(), int[].class);
                inferInput.setData(integerValue, true);
                break;
            case UINT64:
            case INT64:
                long[] longValue = this.gson.fromJson(inputData.getValue(), long[].class);
                inferInput.setData(longValue, true);
                break;
            case FP32:
                float[] floatValue = this.gson.fromJson(inputData.getValue(), float[].class);
                inferInput.setData(floatValue, true);
                break;
            case FP64:
                double[] doubleValue = this.gson.fromJson(inputData.getValue(), double[].class);
                inferInput.setData(doubleValue, true);
                break;
            default:
                break;
            }
            inferInputOptional = Optional.of(inferInput);
        }
        return inferInputOptional;
    }

    private void startInferenceServerClient(String ipServerAddress) {
        try {
            setInferenceServerClient(new InferenceServerClient(ipServerAddress, 5000, 5000));
        } catch (IOException e) {
            logger.error("Cannot connect to Nvidia Triton server {}", ipServerAddress, e);
        }
    }

    private void closeInferenceServerClient() {
        try {
            if (nonNull(this.tritonClient)) {
                this.tritonClient.close();
                this.tritonClient = null;
            }
        } catch (Exception e) {
            logger.error("Cannot close connection to Nvidia Triton Server");
        }
    }

    protected void setInferenceServerClient(InferenceServerClient client) {
        this.tritonClient = client;
    }

    private InferInput createInferInput() {
        return new InferInput(this.options.getInputName(), this.options.getInputSize(),
                DataType.valueOf(this.options.getInputType()));
    }

    private InferResult runInference(List<InferInput> inferInputs, List<InferRequestedOutput> inferOutputs) {
        InferResult result = null;
        try {
            result = tritonClient.infer(this.options.getModelName(), inferInputs, inferOutputs);
        } catch (InferenceException e) {
            logger.error("Failed to run inference for model " + this.options.getModelName(), e);
        }
        return result;
    }

    private short[] convertToShort(int[] intValue) {
        short[] shortValue = new short[intValue.length];
        for (int i = 0; i < shortValue.length; i++) {
            shortValue[i] = (short) intValue[i];
        }
        return shortValue;
    }

    private List<WireRecord> getOutputRecords(InferResult result) {
        List<WireRecord> outputRecords = new ArrayList<>();

        Map<String, TypedValue<?>> properties = new HashMap<>();
        String outputName = this.options.getOutputName();

        switch (DataType.valueOf(this.options.getOutputType())) {
        case BOOL:
            boolean[] booleanOutput = result.getOutputAsBool(outputName);
            for (int i = 0; i < booleanOutput.length; i++) {
                properties.put(outputName + "_" + i, new BooleanValue(booleanOutput[i]));
            }
            break;
        case UINT8:
        case INT8:
            byte[] bytesOutput = result.getOutputAsByte(outputName);
            properties.put(outputName, new ByteArrayValue(bytesOutput));
            break;
        case UINT16:
        case INT16:
            short[] shortOutput = result.getOutputAsShort(outputName);
            for (int i = 0; i < shortOutput.length; i++) {
                properties.put(outputName + "_" + i, new IntegerValue(shortOutput[i]));
            }
            break;
        case UINT32:
        case INT32:
            int[] intOutput = result.getOutputAsInt(outputName);
            for (int i = 0; i < intOutput.length; i++) {
                properties.put(outputName + "_" + i, new IntegerValue(intOutput[i]));
            }
            break;
        case UINT64:
        case INT64:
            long[] longOutput = result.getOutputAsLong(outputName);
            for (int i = 0; i < longOutput.length; i++) {
                properties.put(outputName + "_" + i, new LongValue(longOutput[i]));
            }
            break;
        case FP32:
            float[] floatOutput = result.getOutputAsFloat(outputName);
            for (int i = 0; i < floatOutput.length; i++) {
                properties.put(outputName + "_" + i, new FloatValue(floatOutput[i]));
            }
            break;
        case FP64:
            double[] doubleOutput = result.getOutputAsDouble(outputName);
            for (int i = 0; i < doubleOutput.length; i++) {
                properties.put(outputName + "_" + i, new DoubleValue(doubleOutput[i]));
            }
            break;
        default:
            break;
        }

        WireRecord outputRecord = new WireRecord(properties);
        outputRecords.add(outputRecord);
        return outputRecords;
    }
}
