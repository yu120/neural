package cn.micro.neural.storage;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * OriginalContext
 *
 * @author lry
 */
@Data
@ToString
public class OriginalContext implements Serializable {

    private static final InheritableThreadLocal<OriginalContext> THREAD_LOCAL = new InheritableThreadLocal<>();

    public static void set(OriginalContext originalContext) {
        THREAD_LOCAL.set(originalContext);
    }

    public static OriginalContext get() {
        return THREAD_LOCAL.get();
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

    private final Map<String, Object> attachments = new HashMap<>();

}
