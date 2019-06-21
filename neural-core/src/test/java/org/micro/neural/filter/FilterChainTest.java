package org.micro.neural.filter;

import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * PRO:1->3->2<br>
 * POST:2->1->3<br>
 * ERROR:3->2->1<br>
 * DEFAULT:3->2->1<br>
 *
 * @author lry
 */
public class FilterChainTest {

    @SuppressWarnings("rawtypes")
    @Test
    public void testDefaultFilter() {
        try {
            FilterChain<Message> filterChain = new FilterChain<>();
            for (Map.Entry<String, List<Filter<Message>>> entry : filterChain.getFilters().entrySet()) {
                System.out.println(entry.getKey() + "--->" + entry.getValue());
            }
            System.out.println("===========");
            filterChain.doChain(new Message());

            System.out.println("===========");
            filterChain.doChain(new Message(), "PRE");

            System.out.println("===========");
            filterChain.doChain(new Message(), "POST");

            System.out.println("===========");
            filterChain.doChain(new Message(), "ERROR");

            System.out.println("===========");
            filterChain.doCompositeChain(new Message());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
