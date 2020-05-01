package cn.micro.neural.idempotent.spring;

import cn.micro.neural.storage.FactoryStorage;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@Service
public class TokenServiceImpl implements TokenService {

    private static final String TOKEN_NAME = "IDEMPOTENT_TOKEN";
    private static final String TOKEN_PREFIX = "IDEMPOTENT";


    /**
     * 创建token
     *
     * @return
     */
    @Override
    public String createToken() {
        String str = UUID.randomUUID().toString();
        StringBuilder token = new StringBuilder();
        try {
            token.append(TOKEN_PREFIX).append(str);
            FactoryStorage.INSTANCE.getStorage().setEx(token.toString(), token.toString(), 10000L);
            return token.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    /**
     * 检验token
     *
     * @param request
     * @return
     */
    @Override
    public boolean checkToken(HttpServletRequest request) throws Exception {
        String token = request.getHeader(TOKEN_NAME);
        // header中不存在token
        if (token == null || token.length() == 0) {
            token = request.getParameter(TOKEN_NAME);
            // parameter中也不存在token
            if (token == null || token.length() == 0) {
                throw new RuntimeException("");
            }
        }

        if (!FactoryStorage.INSTANCE.getStorage().exists(token)) {
            throw new RuntimeException("");
        }

        boolean remove = FactoryStorage.INSTANCE.getStorage().remove(token);
        if (!remove) {
            throw new RuntimeException("");
        }

        return true;
    }
}
