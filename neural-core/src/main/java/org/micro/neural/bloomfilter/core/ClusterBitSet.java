package org.micro.neural.bloomfilter.core;

import org.micro.neural.config.store.StorePool;
import org.micro.neural.extension.Extension;

/**
 * The cluster bloom filter on redis bit set.
 *
 * @author lry
 */
@Extension("cluster")
public class ClusterBitSet implements NeuralBitSet {

    private String name;
//    private IStore store = StorePool.getInstance().getStore();

    /**
     * Create a redis BitSet.
     *
     * @param name the redis bit key name.
     */
    public void init(String name) {
        this.name = name;
//        this.store = StorePool.getInstance().getStore();
    }

    @Override
    public void set(int bitIndex) {
//        StatefulRedisConnection<String, String> connection = null;
//        try {
//            connection = store.borrowObject();
//            RedisCommands<String, String> commands = connection.sync();
//            commands.setbit(this.name, bitIndex, 1);
//        } finally {
//            store.returnObject(connection);
//        }
    }

    @Override
    public void set(int bitIndex, boolean value) {
//        StatefulRedisConnection<String, String> connection = null;
//        try {
//            connection = store.borrowObject();
//            RedisCommands<String, String> commands = connection.sync();
//            commands.setbit(this.name, bitIndex, value ? 1 : 0);
//        } finally {
//            store.returnObject(connection);
//        }
    }

    @Override
    public boolean get(int bitIndex) {
//        StatefulRedisConnection<String, String> connection = null;
//        try {
//            connection = store.borrowObject();
//            RedisCommands<String, String> commands = connection.sync();
//            return commands.getbit(this.name, bitIndex) > 0;
//        } finally {
//            store.returnObject(connection);
//        }
        return false;
    }

    @Override
    public void clear(int bitIndex) {
//        StatefulRedisConnection<String, String> connection = null;
//        try {
//            connection = store.borrowObject();
//            RedisCommands<String, String> commands = connection.sync();
//            commands.setbit(this.name, bitIndex, 0);
//        } finally {
//            store.returnObject(connection);
//        }
    }

    @Override
    public void clear() {
//        StatefulRedisConnection<String, String> connection = null;
//        try {
//            connection = store.borrowObject();
//            RedisCommands<String, String> commands = connection.sync();
//            commands.del(this.name);
//        } finally {
//            store.returnObject(connection);
//        }
    }

    @Override
    public long size() {
//        StatefulRedisConnection<String, String> connection = null;
//        try {
//            connection = store.borrowObject();
//            RedisCommands<String, String> commands = connection.sync();
//            return commands.bitcount(this.name);
//        } finally {
//            store.returnObject(connection);
//        }
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return size() <= 0;
    }

}
