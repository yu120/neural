package org.micro.neural.config.store;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Redis Model
 *
 * @author lry
 */
@Getter
@AllArgsConstructor
public enum RedisModel {

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