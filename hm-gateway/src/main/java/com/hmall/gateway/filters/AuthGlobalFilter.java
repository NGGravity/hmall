package com.hmall.gateway.filters;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.hmall.common.exception.UnauthorizedException;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    private final AuthProperties authProperties;

    private final JwtTool jwtTool;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1 获取request
        ServerHttpRequest request = exchange.getRequest();

        // 2 判断是否要做登录校验
        if (isExclude(request.getPath().toString())) {
            // 放行
            return chain.filter(exchange);
        }

        // 3 获取token
        String token = null;
        List<String> authorization = request.getHeaders().get("Authorization");
        if (CollectionUtils.isNotEmpty(authorization)) {
            token = authorization.get(0);
        }

        // 4 校验并解析token
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException e) {
            // 拦截，给出相应状态码
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 5 传递用户信息
        String userInfo = userId.toString();
        ServerWebExchange serverWebExchange = exchange.mutate()
                .request(builder -> builder.header("user-info", userInfo))
                .build();

        // 6 校验
        return chain.filter(serverWebExchange);
    }

    private boolean isExclude(String string) {
        List<String> excludePaths = authProperties.getExcludePaths();
        return Optional.ofNullable(excludePaths)
                .orElse(Collections.emptyList())
                .stream()
                .anyMatch(path -> antPathMatcher.match(path, string));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
