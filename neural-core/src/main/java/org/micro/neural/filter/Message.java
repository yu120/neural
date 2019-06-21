package org.micro.neural.filter;

import lombok.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The Message
 *
 * @author lry
 */
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Message implements Serializable {

    private static final long serialVersionUID = 5386765734933377597L;

    private Long time = System.currentTimeMillis();
    private final Map<String, Object> headers = new HashMap<>();
    private Object body;

    public void addHeader(String key, Object value) {
        headers.put(key, value);
    }

    public void addHeaders(Map<String, Object> headers) {
        headers.putAll(headers);
    }

}
