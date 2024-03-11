package com.CloudShare;

import com.CloudShare.component.RedisComponent;
import com.CloudShare.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;

@Component("initRun")
public class InitRun implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(InitRun.class);

    @Resource
    private DataSource dataSource;

    @Resource
    private RedisComponent redisComponent;

    @Override
    public void run(ApplicationArguments args) {
        try {
            dataSource.getConnection();
        } catch (Exception e) {
            logger.error("数据库连接失败，请检查配置");
            throw new BusinessException("服务启动失败");
        }

        try {
            redisComponent.getSysSettingsDto();
        } catch (Exception e) {
            logger.error("redis连接失败，请检查配置");
            throw new BusinessException("服务启动失败");
        }

        logger.error("服务启动成功，可以开始愉快的开发了");
    }
}
