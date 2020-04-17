package cn.micro.neural.limiter;

import lombok.Data;

import java.io.Serializable;

@Data
public class DemoUser implements Serializable {
    private String id;
    private String name;
    private int age;
}
