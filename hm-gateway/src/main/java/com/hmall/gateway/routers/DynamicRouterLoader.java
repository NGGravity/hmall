package com.hmall.gateway.routers;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.hmall.common.utils.CollUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouterLoader {
    private final NacosConfigManager nacosConfigManager;

    private final RouteDefinitionWriter routeDefinitionWriter;

    private final String dataId = "gateway-router.json";

    private final String group = "DEFAULT_GROUP";

    private final Set<String> routeIds = new HashSet<>(); // 保存更新过的路由id

    @PostConstruct
    public void initRouterConfigListener() throws NacosException {
        String configInfo = nacosConfigManager.getConfigService().getConfigAndSignListener(dataId, group, 5000, new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                // 初始化路由信息
                updateConfigInfo(configInfo);
            }
        });

        // 监听路由变更，更改路由信息
        updateConfigInfo(configInfo);
    }

    private void updateConfigInfo(String configInfo) {
        log.debug("update config info: {}", configInfo);
        // 1 反序列化
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        // 2 更新前先清空旧路由
        // 2.1 清除旧路由
        routeIds.stream().map(Mono::just).forEach(routeId -> routeDefinitionWriter.delete(routeId).subscribe());
        routeIds.clear();
        // 2.2 判断是否有新的路由要更新
        if (CollUtils.isEmpty(routeDefinitions)) {
            return;
        }
        // 3 更新路由
        routeDefinitions.stream().map(Mono::just).forEach(route -> {routeDefinitionWriter.save(route).subscribe();});
    }
}
