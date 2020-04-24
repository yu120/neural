package cn.micro.neural.idempotent;

import javax.servlet.http.HttpServletRequest;

public interface TokenService {

    /**
     * 创建token
     *
     * @return
     */
    String createToken();

    /**
     * 检验token
     *
     * @param request
     * @return
     */
    boolean checkToken(HttpServletRequest request) throws Exception;

}