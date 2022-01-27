package org.eclipse.kura.ai.triton.server;

import static java.util.Objects.isNull;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.ai.inference.engine.InferenceEngineService;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TritonServerServiceImpl implements InferenceEngineService, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(TritonServerServiceImpl.class);

    private CommandExecutorService executorService;
    private TritonServerServiceOptions options;
    private Command serverCommand;

    public void setCommandExecutorService(CommandExecutorService executorService) {
        if (isNull(this.executorService)) {
            this.executorService = executorService;
        }
    }

    public void unsetCommandExecutorService(CommandExecutorService executorService) {
        if (this.executorService == executorService) {
            this.executorService = null;
        }
    }

    protected void activate(Map<String, Object> properties) {
        logger.info("Activate TritonServerService...");
        this.options = new TritonServerServiceOptions(properties);
        startServer();
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Update TritonServerService...");
        stopServer();
        this.options = new TritonServerServiceOptions(properties);
        startServer();
    }

    protected void deactivate() {
        logger.info("Deactivate TritonServerService...");
        stopServer();
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

    private void startServer() {
        if (this.options.isEnabled()) {
            this.serverCommand = createServerCommand();
            this.executorService.execute(serverCommand, status -> {
                if (status.getExitStatus().isSuccessful()) {
                    logger.info("Nvidia Triton Server started");
                } else {
                    logger.info("Nvidia Triton Server not started. Exit value: {}",
                            status.getExitStatus().getExitCode());
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("execute command {} :: exited with code - {}", this.serverCommand,
                            status.getExitStatus().getExitCode());
                    logger.debug("execute stderr {}",
                            new String(((ByteArrayOutputStream) this.serverCommand.getErrorStream()).toByteArray(),
                                    Charsets.UTF_8));
                    logger.debug("execute stdout {}",
                            new String(((ByteArrayOutputStream) this.serverCommand.getOutputStream()).toByteArray(),
                                    Charsets.UTF_8));
                }
            });
        }
    }

    private void stopServer() {
        this.executorService.kill(this.serverCommand.getCommandLine(), LinuxSignal.SIGINT);
    }

    private Command createServerCommand() {
        List<String> commandString = new ArrayList<>();
        commandString.add("tritonserver");
        commandString.add("--model-repository=" + this.options.getModelRepositoryPath());
        commandString.add("--backend-directory=" + this.options.getBackendsPath());
        this.options.getBackendsConfigs().forEach(config -> commandString.add("--backend-config=" + config));
        commandString.add("--http-port=" + this.options.getHttpPorts());
        commandString.add("--grpc-port=" + this.options.getGrpcPorts());
        commandString.add("--metrics-port=" + this.options.getMetricsPorts());
        Command command = new Command(commandString.toArray(new String[0]));
        command.setTimeout(120);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        command.setErrorStream(err);
        command.setOutputStream(out);
        return command;
    }

}
