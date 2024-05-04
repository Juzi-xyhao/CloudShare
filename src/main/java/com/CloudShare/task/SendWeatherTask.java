package com.CloudShare.task;

import com.CloudShare.component.RedisComponent;
import com.CloudShare.entity.config.AppConfig;
import com.CloudShare.entity.dto.SysSettingsDto;
import com.CloudShare.entity.pojo.Weather;
import com.CloudShare.exception.BusinessException;
import com.CloudShare.service.impl.EmailCodeServiceImpl;
import com.CloudShare.utils.OKHttpUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * @projectName: CloudShare-java
 * @package: com.CloudShare.task
 * @author: xyhao
 * @date: 2024/2/14 0:18
 * @Description:
 */
@Component
public class SendWeatherTask {

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    private String cityName;

    private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);

    @Scheduled(cron = "00 00 22 * * ?")
    public void sendWeatherToMyEmail() {
        String toEmail = appConfig.getSendUserName();
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(toEmail);
            helper.setTo(appConfig.getAdminEmails());
            helper.setText(getTomorrowWeatherByCityCode(redisComponent.getSysSettingsDto().getCityCode()));

            helper.setSubject(String.format(appConfig.getCityWeatherTopic(),cityName));
            helper.setSentDate(new Date());

            javaMailSender.send(message);
        } catch (Exception e) {
            logger.error("定时天气邮件发送失败", e);
            throw new BusinessException("定时天气邮件发送失败");
        }
    }

    private String getTomorrowWeatherByCityCode(String CityCode) {
        String apiUrl = String.format("http://t.weather.itboy.net/api/weather/city/%s", CityCode);
        StringBuilder json = new StringBuilder();
        try {
            URL urlObject = new URL(apiUrl);
            URLConnection uc = urlObject.openConnection();
            uc.setRequestProperty("User-Agent", "Mozilla/4.76");
            BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream(), "utf-8"));
            String inputLine = null;
            while ((inputLine = in.readLine()) != null) {
                json.append(inputLine);
            }
            in.close();

//            String responseJson = OKHttpUtils.getRequest(apiUrl);
            String responseJson = json.toString();
            JSONObject data = JSON.parseObject(responseJson).getJSONObject("data");
            JSONObject cityInfo = JSON.parseObject(responseJson).getJSONObject("cityInfo");
            JSONArray cityWeather = (JSONArray) data.get("forecast");
            String cityTomorrowWeather = cityWeather.get(1).toString();

            Weather weather = JSON.parseObject(cityTomorrowWeather, Weather.class);
            weather.setCity(cityInfo.getString("city"));
            cityName = weather.getCity();
            weather.setTime(JSON.parseObject(responseJson).getString("time"));
            weather.setShidu(data.getString("shidu"));
            weather.setQuality(data.getString("quality"));
            weather.setWendu(data.getString("wendu"));

            return getWeatherContent(weather);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static String getWeatherContent(Weather weather) {
        LocalDateTime currentDate = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH时mm分ss秒", Locale.CHINA);
        // 使用format方法将Date对象格式化为字符串
        String formattedDate = formatter.format(currentDate);

        return "静谧的夜幕降临了，星光点点，宁静如水。\n"
                + "此刻，" + formattedDate+"，我愿与你分享" + weather.getCity() + "明日的天气~\n"
                + "温度范围: " + weather.getLow() + "- " + weather.getHigh() + "\n"
                + "空气质量: " + weather.getQuality() + "\n"
                + "太阳将于" + weather.getSunrise() + "准时升起，也将于" + weather.getSunset() + "准时落下\n"
                + "风向是: " + weather.getFx() + "，风速" + weather.getFl() + "\n"
                + "天气是"+ weather.getType()+"\n"
                + "温馨提醒:"+ weather.getNotice()+"\n\n\n\n\n"
                + "最后，我想告诉你的是，秋招一定会有好结果的！不要放弃，坚持努力！";
    }
}
