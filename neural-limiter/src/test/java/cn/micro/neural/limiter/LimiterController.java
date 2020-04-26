package cn.micro.neural.limiter;

import cn.micro.neural.limiter.spring.LimitType;
import cn.micro.neural.limiter.spring.NeuralLimiter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class LimiterController {

    private static final AtomicInteger ATOMIC_INTEGER_1 = new AtomicInteger();
    private static final AtomicInteger ATOMIC_INTEGER_2 = new AtomicInteger();
    private static final AtomicInteger ATOMIC_INTEGER_3 = new AtomicInteger();

    @NeuralLimiter(value = "'角色id为'+#key+#demoUser.name")
    @RequestMapping("/limitTest1")
    public int testLimiter1(@RequestParam("key") String key, @RequestBody DemoUser demoUser) {
        return ATOMIC_INTEGER_1.incrementAndGet();
    }

    @NeuralLimiter(value = "customer_limit_test", type = LimitType.CUSTOMER)
    @RequestMapping("/limitTest2")
    public int testLimiter2() {
        return ATOMIC_INTEGER_2.incrementAndGet();
    }

    @NeuralLimiter(value = "ip_limit_test", type = LimitType.IP)
    @RequestMapping("/limitTest3")
    public int testLimiter3() {
        return ATOMIC_INTEGER_3.incrementAndGet();
    }

}