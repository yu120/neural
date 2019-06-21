package org.micro.neural.config.store;

/**
 * The Store Listener
 *
 * @author lry
 **/
public interface IStoreListener {

    /**
     * The notify
     *
     * @param identity config identity
     * @param data     config message data
     */
    void notify(String identity, String data);

}