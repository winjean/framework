package com.winjean.controller;

import com.winjean.common.BaseResponse;
import com.winjean.model.entity.ConfigEntity;
import com.winjean.service.ConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;



@RestController
@Api("配置信息")
@RequestMapping("/config")
@Slf4j
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @PostMapping("searchConfigs")
    @ApiOperation("查询批量配置信息")
    public Object searchUsers() {
        try {
            List<ConfigEntity> configs = configService.searchConfigs();
            return configs;
        }catch (Exception e){
            e.printStackTrace();
            return BaseResponse.getFailureResponse(e.getMessage());
        }
    }

    @PostMapping(value = "searchConfig")
    @ApiOperation("查询配置信息")
    public Object searchUser(@RequestBody ConfigEntity config) {
        try {
            log.info("receive value : " + config);
            config = configService.searchConfig(config);
            return config;
        }catch (Exception e){
            e.printStackTrace();
            return BaseResponse.getFailureResponse(e.getMessage());
        }
    }
}
