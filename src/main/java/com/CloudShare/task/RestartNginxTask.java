package com.CloudShare.task;

import com.CloudShare.utils.ProcessUtils;
import com.CloudShare.utils.ScaleFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class RestartNginxTask {
    private static final Logger logger = LoggerFactory.getLogger(RestartNginxTask.class);
    @Scheduled(cron = "0 0 */1 * * ?")
    public void run(){
        try {
            String cmd = "systemctl restart nginx";
            ProcessUtils.executeCommand(cmd, true);
        } catch (Exception e) {
            logger.error("重启nginx失败", e);
        }
    }
}
