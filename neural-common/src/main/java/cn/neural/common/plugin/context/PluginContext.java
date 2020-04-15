package cn.neural.common.plugin.context;

import java.util.Set;

/**
 * 上下文对象
 *
 * @author lry
 */
public interface PluginContext {

    Object getAttribute(String attr);

    Set<String> getAttributeNames();

    void setAttribute(String attr, Object value);

    void removeAttribute(String attr);

}
