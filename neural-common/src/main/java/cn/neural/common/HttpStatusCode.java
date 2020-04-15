package cn.neural.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * HttpStatusCode
 *
 * @author lry
 */
@Getter
@AllArgsConstructor
public enum HttpStatusCode {

    // ===

    OK(200, "成功"),
    NO_CONTENT(204, "无内容"),
    BAD_REQUEST(400, "服务器不理解客户端的请求（如，参数错误）"),
    UNAUTHORIZED(401, "没有通过身份验证"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "资源不存在，或不可用"),
    METHOD_NOT_ALLOWED(405, "对于此HTTP方法没有权限"),
    CONFLICT(409, "通用冲突"),
    GONE(410, "资源不存在，或不可用"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    TOO_MANY_REQUESTS(429, "请求次数超过限额"),
    INTERNAL_SERVER_ERROR(500, "服务器通用错误"),
    SERVICE_UNAVAILABLE(503, "服务端当前无法处理请求");

    private final int code;
    private final String msg;

}
