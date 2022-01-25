/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.ai.inference.engine;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The InferenceEngineService interface is a service API for managing Inference Engines
 * for Artificial Intelligence and Machine Learning algorithms
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.3
 */
@ProviderType
public interface InferenceEngineService {

    /*
     * Load a model in to the system
     */
    public void loadModel(String modelName);

    /*
     * Delete a model from the system
     */
    public void deleteModel(String modelName);

    /*
     * Return a list of the names of models available in the system
     */
    public List<String> getModelNames();

    /*
     * Return information about a specified model
     */
    public void getModelInfo(String modelName);

    /*
     * Perform the inference for a given model
     */
    public byte[] infer(String modelName);

}
