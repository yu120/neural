package org.micro.neural.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;

@Controller
public class WebController {

    @RequestMapping("/")
    public String index(HashMap<String, Object> map) {
        map.put("hello", "欢迎进入HTML页面");
        return "index";
    }

    @RequestMapping("main")
    public String main(HashMap<String, Object> map) {
        return "main";
    }

    @RequestMapping("limiter-configs")
    public String limiterConfigs(HashMap<String, Object> map) {
        return "limiter-configs";
    }

    @RequestMapping("limiter-config")
    public String limiterConfig(HashMap<String, Object> map) {
        return "limiter-config";
    }

    @RequestMapping("limiter-monitor")
    public String limiterMonitor(HashMap<String, Object> map) {
        return "limiter-monitor";
    }

    @RequestMapping("degrade-configs")
    public String degradeConfigs(HashMap<String, Object> map) {
        return "degrade-configs";
    }

    @RequestMapping("degrade-config")
    public String degradeConfig(HashMap<String, Object> map) {
        return "degrade-config";
    }

}
