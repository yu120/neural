package org.micro.neural.config;

import lombok.*;
import org.micro.neural.common.utils.NetUtils;

import java.io.Serializable;

/**
 * The Node Config.
 *
 * @author lry
 **/
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class NodeConfig implements Serializable {

    private static final long serialVersionUID = 3749338575377195865L;

    /**
     * The host of node
     */
    private String hostName = NetUtils.getLocalAddress().getHostName();
    /**
     * The ip of node
     */
    private String hostAddress = NetUtils.getLocalAddress().getHostAddress();
    /**
     * The port of node
     */
    private String port;
    /**
     * The process id of node
     */
    private String processId = NetUtils.getProcessId();
    /**
     * The current time id of node
     */
    private String time = String.valueOf(System.currentTimeMillis());

}