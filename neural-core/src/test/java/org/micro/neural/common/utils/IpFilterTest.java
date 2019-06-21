package org.micro.neural.common.utils;

import org.micro.neural.common.micro.IpFilter;

public class IpFilterTest {

	public static void main(String[] args) {
        String ipWhilte = "192.168.1.1;" +                 //设置单个IP的白名单
                          "192.168.2.*;" +                 //设置ip通配符,对一个ip段进行匹配
                          "192.168.3.17-192.168.3.38";     //设置一个IP范围
        boolean flag = IpFilter.containIp("192.168.2.2",ipWhilte);
        boolean flag2 = IpFilter.containIp("192.168.1.2",ipWhilte);
        boolean flag3 = IpFilter.containIp("192.168.3.16",ipWhilte);
        boolean flag4 = IpFilter.containIp("192.168.3.17",ipWhilte);
        System.out.println(flag);  //true
        System.out.println(flag2);  //false
        System.out.println(flag3);  //false
        System.out.println(flag4);  //true
    }
	
}
