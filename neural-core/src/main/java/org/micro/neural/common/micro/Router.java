package org.micro.neural.common.micro;

import org.micro.neural.common.URL;
import org.micro.neural.common.collection.KeyValueStore;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * 流量路由<br>
 * <br>
 * 主要解决两个场景：<br>
 * 1.灰度路由<br>
 * 2.分组路由<br>
 * <br>
 * 要求：<br>
 * 1.不需要重启<br>
 * 2.秒级生效<br>
 * <br>
 * 主要流程：<br>
 * 1.管控台定义消费者及其特性属性<br>
 * 2.consumerId,Map<consumerId,特征属性集> ==> 1组特征属性集<br>
 * 3.1组特征属性集,Map<rule,> ==> <br>
 * 4.<br>
 * 5.<br>
 *
 * @author lry
 */
public class Router {

    /**
     * 神经路由
     *
     * @param consumerId 消费者ID
     * @param serviceId  本次需要消费的服务ID
     * @param categories 消费者特征属性集
     * @param rules      路由规则集
     * @return
     */
    public KeyValueStore<String, String> doRoute(
            String consumerId, String serviceId,
            ConcurrentMap<String, ConcurrentMap<String, String>> categories,
            ConcurrentMap<String, ConcurrentMap<String, KeyValueStore<String, String>>> rules) {
        // 第一步：根据ID查找该用户的特征属性集
        ConcurrentMap<String, String> tempCategories = this.selectCategories(consumerId, categories);

        // 第二步：根据一组特征属性集和路由规则,复合计算出一组可用服务信息集：Map<serviceId, {group,version}>
        ConcurrentMap<String, KeyValueStore<String, String>> services = this.compositeCalculation(tempCategories, rules);

        // 第三步：根据需要消费的服务ID和可用服务信息集查找出需要消费的服务信息：{group,version}
        KeyValueStore<String, String> service = this.selectService(serviceId, services);

        return service;
    }

    /**
     * 第一步：根据ID查找该用户的特征属性集
     *
     * @param consumerId
     * @param categories
     * @return
     */
    private ConcurrentMap<String, String> selectCategories(
            String consumerId, ConcurrentMap<String, ConcurrentMap<String, String>> categories) {
        return categories.get(consumerId);
    }

    /**
     * 第二步：根据一组特征属性集和路由规则,复合计算出一组可用服务信息集：Map<serviceId, {group,version}>
     *
     * @param categories
     * @param rules
     * @return
     */
    private ConcurrentMap<String, KeyValueStore<String, String>> compositeCalculation(
            ConcurrentMap<String, String> categories,
            ConcurrentMap<String, ConcurrentMap<String, KeyValueStore<String, String>>> rules) {
        for (Map.Entry<String, ConcurrentMap<String, KeyValueStore<String, String>>> entry : rules.entrySet()) {
            URL url = URL.valueOf("/?" + entry.getKey());
            for (Map.Entry<String, String> tempEntry : url.getParameters().entrySet()) {
                System.out.println(tempEntry.getKey());
            }
        }

        String key = null;
        return rules.get(key);
    }

    /**
     * 第三步：根据需要消费的服务ID和可用服务信息集查找出需要消费的服务信息：{group,version}
     *
     * @param serviceId
     * @param services
     * @return
     */
    private KeyValueStore<String, String> selectService(
            String serviceId, ConcurrentMap<String, KeyValueStore<String, String>> services) {
        return services.get(serviceId);
    }

}
