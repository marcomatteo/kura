package org.eclipse.kura.ai.triton.server;

import static org.junit.Assert.assertTrue;

import org.eclipse.kura.ai.inference.engine.InferenceEngineService;
import org.eclipse.kura.ai.triton.server.TritonServerServiceImpl;
import org.junit.Test;

public class TritonServerServiceImplTest {

    private String modelName = "iris";
    private InferenceEngineService engine;
    private byte[] result;

    @Test
    public void shouldLoadModel() {

    }

    @Test
    public void shouldDeleteModel() {

    }

    @Test
    public void shouldGetModelNames() {

    }

    @Test
    public void shouldGetModelInfo() {

    }

    @Test
    public void shouldInfer() {
        givenInferenceEngine();

        whenInfer();

        thenResultIsNotEmpty();
    }

    private void givenInferenceEngine() {
        this.engine = new TritonServerServiceImpl();
    }

    private void whenInfer() {
        this.result = this.engine.infer(this.modelName);
    }

    private void thenResultIsNotEmpty() {
        assertTrue(this.result.length > 0);
    }
}
