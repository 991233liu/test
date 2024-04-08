package com.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.alibaba.fastjson.JSON;

/**
 * 审计日志，记录返回值
 *
 */
@ControllerAdvice
public class AuditGlobalResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    private final static Logger audit = LoggerFactory.getLogger("audit.WebSecurity.Response");

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 返回true表示对所有控制器的ResponseBody方法生效，可以根据需要自定义逻辑
        return true;
    }

    @Override
    public Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        if (!audit.isTraceEnabled())
            return body;

        Integer status = null;
        String uri = request.getURI().getRawPath();
        if (response instanceof ServletServerHttpResponse) {
            ServletServerHttpResponse servletResponse = (ServletServerHttpResponse) response;
            javax.servlet.http.HttpServletResponse httpServletResponse = servletResponse.getServletResponse();
            status = httpServletResponse.getStatus();
        }
        if (MediaType.APPLICATION_JSON.includes(selectedContentType)) {
            audit.trace("URI: {}\n    ServerHttpResponse.StatusCode: {}\n    ServerHttpResponse.ContentType: {}\n    ServerHttpResponse.Body: {}", uri, status, selectedContentType,
                    JSON.toJSONString(body));
        } else {
            audit.trace("URI: {}\n    ServerHttpResponse.StatusCode: {}\n    ServerHttpResponse.ContentType: {}", uri, status, selectedContentType);
        }
        return body;
    }
}
