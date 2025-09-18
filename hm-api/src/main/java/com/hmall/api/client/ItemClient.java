package com.hmall.api.client;


import com.hmall.api.dto.ItemDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient("item-service") // 服务名称
public interface ItemClient {

    @GetMapping("/items") // 请求方式和请求路径
    List<ItemDTO> queryItemByIds(@RequestParam("ids") Collection<Long> ids); // 返回类型和请求参数
}
