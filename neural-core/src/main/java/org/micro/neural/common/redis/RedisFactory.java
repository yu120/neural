package org.micro.neural.common.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.micro.neural.common.URL;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.*;

import java.util.HashSet;

/**
 * Redis Factory
 *
 * @author lry
 */
@Getter
public enum RedisFactory {

    // ====

    INSTANCE;

    private RedissonClient redissonClient;

    public void initialize(URL url) {
        Config config = new Config();

        String category = url.getParameter(URL.CATEGORY_KEY);
        RedisModel redisModel = RedisModel.parse(category);

        if (RedisModel.SENTINEL == redisModel) {
            SentinelServersConfig sentinelServersConfig = config.useSentinelServers();
            sentinelServersConfig.addSentinelAddress(url.getAddresses());
        } else if (RedisModel.CLUSTER == redisModel) {
            ClusterServersConfig clusterServersConfig = config.useClusterServers();
            clusterServersConfig.addNodeAddress(url.getAddresses());
        } else if (RedisModel.MASTER_SLAVE == redisModel) {
            MasterSlaveServersConfig masterSlaveServersConfig = config.useMasterSlaveServers();
            masterSlaveServersConfig.setMasterAddress(url.getAddress());
            masterSlaveServersConfig.setSlaveAddresses(new HashSet<>(url.getBackupAddressList()));
        } else if (RedisModel.REPLICATED == redisModel) {
            ReplicatedServersConfig replicatedServersConfig = config.useReplicatedServers();
            replicatedServersConfig.addNodeAddress(url.getAddresses());
        } else {
            SingleServerConfig singleServerConfig = config.useSingleServer();
            singleServerConfig.setAddress(url.getAddress());
        }

        this.redissonClient = Redisson.create(config);
    }

    public void destroy() {
        if (null != redissonClient) {
            redissonClient.shutdown();
        }
    }

    @Getter
    @AllArgsConstructor
    enum RedisModel {

        // ===

        SINGLE("single"),
        SENTINEL("sentinel"),
        CLUSTER("cluster"),
        MASTER_SLAVE("master-slave"),
        REPLICATED("replicated");

        String category;

        public static RedisModel parse(String category) {
            if (category == null || category.length() == 0) {
                return SINGLE;
            }

            for (RedisModel e : values()) {
                if (e.getCategory().equals(category)) {
                    return e;
                }
            }

            return SINGLE;
        }

    }

}
