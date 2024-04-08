package com.spring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 认证Filter
 *
 */
@Component
@Order(1)
public class WebSecurityFilter implements Filter {
    private static final Logger log = LogManager.getLogger(WebSecurityFilter.class);

    @Value("${security.appKey:}")
    private String appKey;
    @Value("${security.appSecret:}")
    private String appSecret;
    private List<String> basicToken = new ArrayList<>();
    Base64.Encoder encoder = Base64.getEncoder();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.debug("---------# WebSecurityFilter.init");
        String[] appKeys = appKey.split(";");
        String[] appSecrets = appSecret.split(";");
        for (int i = 0; i < appKeys.length; i++) {
            String str = appKeys[i] + ":" + appSecrets[i];
            basicToken.add("Basic " + encoder.encodeToString(str.getBytes()));
        }
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        log.debug("---------# WebSecurityFilter.doFilter in");

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse rep = (HttpServletResponse) response;

        if (checkToken(req)) {
            chain.doFilter(request, response);
        } else {
            senNoLoginMessage(rep);
        }
        log.debug("---------# WebSecurityFilter.doFilter out");
    }

    @Override
    public void destroy() {
        log.debug("---------# WebSecurityFilter.destroy");
        Filter.super.destroy();
    }

    private boolean checkToken(HttpServletRequest req) {
        String token = req.getHeader("Authorization");
        if (token != null && token.startsWith("Basic ")) {
            // 基于HTTP的“Basic”模式
            for (String str : basicToken) {
                if (token.equals(str))
                    return true;
            }
        }
        return false;
    }

    private void senNoLoginMessage(HttpServletResponse httpServletResponse) throws IOException {
        String str = "{\"code\": 403, \"msg\": \"Unauthorized credentials\"}";
        httpServletResponse.setStatus(HttpStatus.FORBIDDEN.value());
        httpServletResponse.setCharacterEncoding("utf-8");
        httpServletResponse.setHeader("Content-Type", "application/json ");
        httpServletResponse.getWriter().write(str);
    }
}
