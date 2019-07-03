package org.micro.neural;

import java.io.Serializable;
import java.util.UUID;

/**
 * The Neural Context.
 *
 * @author lry
 */
public class NeuralContext implements Serializable {

    private String id = UUID.randomUUID().toString();

}
