package org.eclipse.kura.wire.internal.ai.triton.client;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.StringValue;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonSyntaxException;

import triton.client.InferResult;
import triton.client.InferenceException;
import triton.client.InferenceServerClient;

public class TritonServerClientTest {

    private InferenceServerClient tritonClient;
    private TritonServerClient client;
    private WireHelperService wireHelperService;
    private WireSupport wireSupport;
    List<WireRecord> wireRecords;
    private InferResult inferResult;
    private Map<String, Object> inputConfiguration;
    private Map<String, TypedValue<?>> outputProperties;
    private boolean exceptionOccurred = false;

    @Test
    public void shouldEmitEmptyOutput() throws InferenceException, IOException {
        givenInputConfigurationWithFloat();
        givenTritonServerClientActivated();

        whenReceiveEmptyWire();

        thenEmitEmptyWire();
    }

    @Test
    public void shouldEmitFloatOutput() throws InferenceException, IOException {
        givenInputConfigurationWithFloat();
        givenTritonServerClientActivated();
        givenInferResultWithFloat();

        whenReceiveWireWithFloat();

        thenEmitWireWithFloat();
    }

    @Test
    public void shouldEmitLongOutput() throws InferenceException, IOException {
        givenInputConfigurationWithLong();
        givenTritonServerClientActivated();
        givenInferResultWithLong();

        whenReceiveWireWithLong();

        thenEmitWireWithLong();
    }

    @Test
    public void shouldEmitShortOutput() throws InferenceException, IOException {
        givenInputConfigurationWithShort();
        givenTritonServerClientActivated();
        givenInferResultWithShort();

        whenReceiveWireWithShort();

        thenEmitWireWithInteger();
    }

    @Test
    public void shouldThrowExceptionOnWrongJson() throws InferenceException, IOException {
        givenInputConfigurationWithLong();
        givenTritonServerClientActivated();

        whenReceiveWireWithWrongJson();

        thenExceptionHasOccurred();
    }

    private void givenTritonServerClientActivated() throws InferenceException, IOException {
        this.tritonClient = mock(InferenceServerClient.class);
        this.inferResult = mock(InferResult.class);
        when(this.tritonClient.infer(eq("my_simple_model"), anyObject(), anyObject())).thenReturn(inferResult);
        this.client = new TritonServerClient();
        this.wireHelperService = mock(WireHelperService.class);
        this.wireSupport = mock(WireSupport.class);
        when(wireHelperService.newWireSupport(anyObject(), anyObject())).thenReturn(this.wireSupport);
        this.client.setWireHelperService(wireHelperService);

        ComponentContext cc = mock(ComponentContext.class);
        this.client.activate(cc, this.inputConfiguration);
        this.client.setInferenceServerClient(tritonClient);

        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            List<WireRecord> wireRecords = (List<WireRecord>) arguments[0];
            if (!wireRecords.isEmpty()) {
                this.outputProperties = wireRecords.get(0).getProperties();
            }
            return null;
        }).when(this.wireSupport).emit(any());
    }

    private void givenInputConfigurationWithFloat() {
        createBasicInputConfiguration();
        inputConfiguration.put("input.type", "FP32");
        inputConfiguration.put("input.size", "1,2");
        inputConfiguration.put("output.type", "FP32");
    }

    private void givenInputConfigurationWithLong() {
        createBasicInputConfiguration();
        inputConfiguration.put("input.type", "INT64");
        inputConfiguration.put("input.size", "1,4");
        inputConfiguration.put("output.type", "INT64");
    }

    private void givenInputConfigurationWithShort() {
        createBasicInputConfiguration();
        inputConfiguration.put("input.type", "INT16");
        inputConfiguration.put("input.size", "1,4");
        inputConfiguration.put("output.type", "INT16");
    }

    private void createBasicInputConfiguration() {
        this.inputConfiguration = new HashMap<>();
        inputConfiguration.put("server.address", "10.235.0.11");
        inputConfiguration.put("server.port", "8023");
        inputConfiguration.put("model.name", "my_simple_model");
        inputConfiguration.put("input.name", "my_input");
        inputConfiguration.put("output.name", "my_output");
    }

    private void givenInferResultWithFloat() {
        when(this.inferResult.getOutputAsFloat("my_output")).thenReturn(new float[] { 9.9F });
    }

    private void givenInferResultWithLong() {
        when(this.inferResult.getOutputAsLong("my_output")).thenReturn(new long[] { 56L });
    }

    private void givenInferResultWithShort() {
        when(this.inferResult.getOutputAsShort("my_output")).thenReturn(new short[] { 6 });
    }

    private void whenReceiveEmptyWire() {
        WireEnvelope wireEnvelope = new WireEnvelope("myPid", new ArrayList<>());
        this.client.onWireReceive(wireEnvelope);
    }

    private void whenReceiveWireWithFloat() {
        receiveWire("[1.4, 2.5]");
    }

    private void whenReceiveWireWithLong() {
        receiveWire("[1, 2, 3, 4]");
    }

    private void whenReceiveWireWithShort() {
        receiveWire("[5, 6, 7, 19]");
    }

    private void whenReceiveWireWithWrongJson() {
        receiveWire("1, 2, 3, 4");
    }

    private void receiveWire(String value) {
        try {
            List<WireRecord> wireRecords = new ArrayList<>();
            Map<String, TypedValue<?>> properties = new HashMap<>();
            properties.put("my_input", new StringValue(value));
            WireRecord wireRecord = new WireRecord(properties);
            wireRecords.add(wireRecord);
            WireEnvelope wireEnvelope = new WireEnvelope("myPid", wireRecords);
            this.client.onWireReceive(wireEnvelope);
        } catch (JsonSyntaxException e) {
            this.exceptionOccurred = true;
        }
    }

    private void thenEmitEmptyWire() {
        verify(this.wireSupport, times(1)).emit(new ArrayList<>());
    }

    private void thenEmitWireWithFloat() {
        assertEquals(1, this.outputProperties.size());
        assertEquals(9.9F, (float) this.outputProperties.get("my_output_0").getValue(), 0.1F);
    }

    private void thenEmitWireWithLong() {
        assertEquals(1, this.outputProperties.size());
        assertEquals(56L, (long) this.outputProperties.get("my_output_0").getValue());
    }

    private void thenEmitWireWithInteger() {
        assertEquals(1, this.outputProperties.size());
        assertEquals(6, (int) this.outputProperties.get("my_output_0").getValue());
    }

    private void thenExceptionHasOccurred() {
        assertTrue(this.exceptionOccurred);
    }
}
