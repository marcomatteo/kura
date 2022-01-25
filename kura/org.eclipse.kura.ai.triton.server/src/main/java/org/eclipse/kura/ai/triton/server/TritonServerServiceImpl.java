package org.eclipse.kura.ai.triton.server;

import java.util.List;
import java.util.Map;

import org.eclipse.kura.ai.inference.engine.InferenceEngineService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TritonServerServiceImpl implements InferenceEngineService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(TritonServerServiceImpl.class);

    protected void activate(Map<String, Object> properties) {
        logger.info("Activate TritonServerService...");

    }

    public void updated(Map<String, Object> properties) {
        logger.info("Update TritonServerService...");

    }

    protected void deactivate() {
        logger.info("Deactivate TritonServerService...");

    }

    @Override
    public void loadModel(String modelName) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteModel(String modelName) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<String> getModelNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void getModelInfo(String modelName) {
        // TODO Auto-generated method stub

    }

    @Override
    public byte[] infer(String modelName) {
        // TODO Auto-generated method stub
        byte[] result = new byte[] { 1 };
        return result;
    }

}
