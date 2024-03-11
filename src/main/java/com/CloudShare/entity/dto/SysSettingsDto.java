package com.CloudShare.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Component
public class SysSettingsDto implements Serializable {
    /**
     * 注册发送邮件标题
     */
    private String registerEmailTitle = "CloudShare验证码";

    /**
     * 注册发送邮件内容
     */
    private String registerEmailContent = "你好，邮箱验证码是：%s，15分钟有效";

    /**
     * 用户初始化空间大小 1GB
     */
    private Integer userInitUseSpace = 1024;

    /**
    * 默认是南昌的天气城市代码
     */
    private  String cityCode = "101240101";


    public String getCityCode() {
        return this.cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getRegisterEmailTitle() {
        return registerEmailTitle;
    }

    public void setRegisterEmailTitle(String registerEmailTitle) {
        this.registerEmailTitle = registerEmailTitle;
    }

    public String getRegisterEmailContent() {
        return registerEmailContent;
    }

    public void setRegisterEmailContent(String registerEmailContent) {
        this.registerEmailContent = registerEmailContent;
    }
    public String setRegisterEmailContent(String code,int i ) {
        this.registerEmailContent = String.format(registerEmailContent,code);
        return registerEmailContent;
    }
    public Integer getUserInitUseSpace() {
        return userInitUseSpace;
    }

    public void setUserInitUseSpace(Integer userInitUseSpace) {
        this.userInitUseSpace = userInitUseSpace;
    }
}
