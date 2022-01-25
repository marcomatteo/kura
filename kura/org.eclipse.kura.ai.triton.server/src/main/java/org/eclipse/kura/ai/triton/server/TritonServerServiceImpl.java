package org.eclipse.kura.ai.triton.server;

import java.util.List;

import org.eclipse.kura.ai.inference.engine.InferenceEngineService;

public class TritonServerServiceImpl implements InferenceEngineService {

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
