package com.CloudShare.controller;

import com.CloudShare.annotation.GlobalInterceptor;
import com.CloudShare.annotation.VerifyParam;
import com.CloudShare.component.RedisComponent;
import com.CloudShare.entity.config.AppConfig;
import com.CloudShare.entity.constants.Constants;
import com.CloudShare.entity.dto.CreateImageCode;
import com.CloudShare.entity.dto.SessionWebUserDto;
import com.CloudShare.entity.enums.ResponseCodeEnum;
import com.CloudShare.entity.enums.VerifyRegexEnum;
import com.CloudShare.entity.pojo.UserInfo;
import com.CloudShare.entity.vo.ResponseVO;
import com.CloudShare.exception.BusinessException;
import com.CloudShare.service.EmailCodeService;
import com.CloudShare.service.UserInfoService;
import com.CloudShare.task.RestartNginxTask;
import com.CloudShare.task.SendWeatherTask;
import com.CloudShare.utils.IPUtil;
import com.CloudShare.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController("accountController")
public class AccountController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    SendWeatherTask sendWeatherTask;


    @RequestMapping("/sendWeatherEmail")
    public Object sendWeatherCode(HttpServletRequest request,HttpServletResponse response) {
        try{
            
            sendWeatherTask.sendWeatherToMyEmail();
            ResponseVO successResponse = new ResponseVO();
            successResponse.setCode(ResponseCodeEnum.CODE_200.getCode());
            successResponse.setInfo("weatherEmail has send。" + StringTools.getRandomString(5));
            successResponse.setStatus(STATUS_SUCCESS);
            return successResponse;
        }catch (Exception e){
            logger.error("地址请求错误，请求地址{},错误信息:", request.getRequestURL(), e);
            ResponseVO errorResponse = new ResponseVO();
            errorResponse.setCode(ResponseCodeEnum.CODE_1000.getCode());
            errorResponse.setInfo("false,please find more info in log files。" +StringTools.getRandomString(5));
            errorResponse.setStatus(STATUS_ERROR);
            return errorResponse;
        }
    }

    /**
     * 验证码
     *
     * @param response
     * @param session
     * @param type
     * @throws IOException
     */
    @RequestMapping(value = "/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws
            IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);

        //设置Pragma头为no-cache，告知浏览器不要缓存这个页面
        response.setHeader("Pragma", "no-cache");

        //设置Cache-Control头为no-cache，这同样是为了防止浏览器缓存页面
        response.setHeader("Cache-Control", "no-cache");

        //设置Expires头为0，表示页面立即过期。这也是为了防止浏览器缓存页面
        response.setDateHeader("Expires", 0);

        //设置Content-Type头为image/jpeg，表示响应的内容类型是JPEG图像
        response.setContentType("image/jpeg");

        String code = vCode.getCode();
        if (type == null || type == 0) {  //0:登录注册  1:邮箱验证码发送 默认0
            session.setAttribute(Constants.CHECK_CODE_KEY, code);

        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    /**
     * @Description: 发送邮箱验证码
     * @auther: Juzi
     * @date: 20:39 2023/9/1
     * @param: [session, email, checkCode, type]
     * @return: com.CloudShare.entity.vo.ResponseVO
     */
    @RequestMapping("/sendEmailCode")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO sendEmailCode(HttpSession session,
                                    HttpServletRequest request,
                                    @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                                    @VerifyParam(required = true) String checkCode,
                                    @VerifyParam(required = true) Integer type) {
        try {
            String ipAddress = IPUtil.getIpAddr(request);
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                throw new BusinessException("图片验证码不正确");  //验证成功后再发送邮件验证码
            }
            emailCodeService.sendEmailCode(email, type,ipAddress);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }


    /**
     * @param session:
     * @param email:
     * @param nickName:
     * @param password:
     * @param checkCode:
     * @param emailCode:
     * @return ResponseVO
     * @author xyhao
     * @description Finished
     * @date 2023/10/20 16:28
     */
    @RequestMapping("/register")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO register(HttpSession session,
                               HttpServletRequest request,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                               @VerifyParam(required = true, max = 20) String nickName,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
                               @VerifyParam(required = true) String checkCode,
                               @VerifyParam(required = true) String emailCode) {
        try {
            String ipAddress = IPUtil.getIpAddr(request);
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            userInfoService.register(email, nickName, password, emailCode,ipAddress);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * @Description: 登录
     * @auther: Juzi
     * @date: 20:39 2023/9/4
     * @param: [session, request, email, password, checkCode]
     * @return: com.CloudShare.entity.vo.ResponseVO
     */
    @RequestMapping("/login")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO login(HttpSession session, HttpServletRequest request,
                            @VerifyParam(required = true) String email,
                            @VerifyParam(required = true) String password,
                            @VerifyParam(required = true) String checkCode) {
        try {
            String ipAddress = IPUtil.getIpAddr(request);
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email, password,ipAddress);
            session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
            return getSuccessResponseVO(sessionWebUserDto);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/resetPwd")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO resetPwd(HttpSession session,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
                               @VerifyParam(required = true) String checkCode,
                               @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            userInfoService.resetPwd(email, password, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/getAvatar/{userId}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public void getAvatar(HttpServletResponse response,
                          @VerifyParam(required = true) @PathVariable("userId") String userId) {
        String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File folder = new File(appConfig.getProjectFolder() + avatarFolderName);
        if (!folder.exists()) {
            folder.mkdirs();  //创建文件夹
        }

        String avatarPath = appConfig.getProjectFolder() + avatarFolderName + userId + Constants.AVATAR_SUFFIX;
        File file = new File(avatarPath);
        if (!file.exists()) {
            if (!new File(appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT).exists()) {
                printNoDefaultImage(response);
                return;
            }
            avatarPath = appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT;
        }
        response.setContentType("image/jpg");
        readFile(response, avatarPath);
    }

    private void printNoDefaultImage(HttpServletResponse response) {
        response.setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
            writer.close();
        } catch (Exception e) {
            logger.error("输出无默认图失败", e);
        } finally {
            writer.close();
        }
    }


    @RequestMapping("/getUserInfo")
    @GlobalInterceptor
    public ResponseVO getUserInfo(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        return getSuccessResponseVO(sessionWebUserDto);
    }

    @RequestMapping("/getUseSpace")
    @GlobalInterceptor
    public ResponseVO getUseSpace(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        return getSuccessResponseVO(redisComponent.getUserSpaceUse(sessionWebUserDto.getUserId()));
    }

    @RequestMapping("/logout")
    public ResponseVO logout(HttpSession session) {
        session.invalidate();
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/updateUserAvatar")
    @GlobalInterceptor
    public ResponseVO updateUserAvatar(HttpSession session, MultipartFile avatar) {  //使用MultipartFile来接收上传的文件
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists()) {
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + webUserDto.getUserId() + Constants.AVATAR_SUFFIX);
        try {
            avatar.transferTo(targetFile);
        } catch (Exception e) {
            logger.error("上传头像失败", e);
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setQqAvatar("");  //设置qq头像，暂未实现
        userInfoService.updateUserInfoByUserId(userInfo, webUserDto.getUserId());
        webUserDto.setAvatar(null);
        session.setAttribute(Constants.SESSION_KEY, webUserDto);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/updatePassword")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO updatePassword(HttpSession session,
                                     @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfoService.updateUserInfoByUserId(userInfo, sessionWebUserDto.getUserId());
        return getSuccessResponseVO(null);
    }



    //qq登录的逻辑：
    // 1、先在QQ备案,获得appid和app key
    // 2、把appid和回调地址以及随机状态码拼接到QQ的跳转链接，形成一个URL
    // 3、QQ返回一个code和之前生成的state
    // 4、根据code拿到access_Token(通过OKHTTPS发送请求)
    // 5、根据access_Token拿到openid
    // 6、根据openid拿到userinfo(nickname,avatar)
    @RequestMapping("qqlogin")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO qqlogin(HttpSession session,
                             String callbackUrl) throws UnsupportedEncodingException {
        String state = StringTools.getRandomString(Constants.LENGTH_30);  //随机数状态码
        if (!StringTools.isEmpty(callbackUrl)) {
            session.setAttribute(state, callbackUrl);
        }
        //                                         QQURL授权
        String url = String.format(appConfig.getQqUrlAuthorization(), appConfig.getQqAppId(), URLEncoder.encode(appConfig.getQqUrlRedirect(), "utf-8"), state);
        return getSuccessResponseVO(url);
    }

    @RequestMapping("qqlogin/callback")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO qqLoginCallback(HttpSession session,
                                      @VerifyParam(required = true) String code,  //QQ返回的code
                                      @VerifyParam(required = true) String state) {
        SessionWebUserDto sessionWebUserDto = userInfoService.qqLogin(code);
        session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
        Map<String, Object> result = new HashMap<>();
        result.put("callbackUrl", session.getAttribute(state));
        result.put("userInfo", sessionWebUserDto);
        return getSuccessResponseVO(result);
    }
}
