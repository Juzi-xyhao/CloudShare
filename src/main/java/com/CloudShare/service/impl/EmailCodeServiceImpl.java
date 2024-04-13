package com.CloudShare.service.impl;

import com.CloudShare.component.RedisComponent;
import com.CloudShare.entity.config.AppConfig;
import com.CloudShare.entity.constants.Constants;
import com.CloudShare.entity.dto.SysSettingsDto;
import com.CloudShare.entity.enums.PageSize;
import com.CloudShare.entity.pojo.EmailCode;
import com.CloudShare.entity.pojo.UserInfo;
import com.CloudShare.entity.query.EmailCodeQuery;
import com.CloudShare.entity.query.SimplePage;
import com.CloudShare.entity.query.UserInfoQuery;
import com.CloudShare.entity.vo.PageInfoResultVO;
import com.CloudShare.exception.BusinessException;
import com.CloudShare.mappers.EmailCodeMapper;
import com.CloudShare.mappers.UserInfoMapper;
import com.CloudShare.service.EmailCodeService;
import com.CloudShare.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.List;


/**
 * 邮箱验证码 业务接口实现
 */
@Service("emailCodeService")
public class EmailCodeServiceImpl implements EmailCodeService {

    private static final Logger logger = LoggerFactory.getLogger(EmailCodeServiceImpl.class);

    @Resource
    private EmailCodeMapper<EmailCode, EmailCodeQuery> emailCodeMapper;

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private AppConfig appConfig;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<EmailCode> findListByParam(EmailCodeQuery param) {
        return this.emailCodeMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(EmailCodeQuery param) {
        return this.emailCodeMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PageInfoResultVO<EmailCode> findListByPage(EmailCodeQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<EmailCode> list = this.findListByParam(param);
        PageInfoResultVO<EmailCode> result = new PageInfoResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(EmailCode bean) {
        return this.emailCodeMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<EmailCode> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.emailCodeMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<EmailCode> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.emailCodeMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据EmailAndCode获取对象
     */
    @Override
    public EmailCode getEmailCodeByEmailAndCode(String email, String code) {
        return this.emailCodeMapper.selectByEmailAndCode(email, code);
    }

    /**
     * 根据EmailAndCode修改
     */
    @Override
    public Integer updateEmailCodeByEmailAndCode(EmailCode bean, String email, String code) {
        return this.emailCodeMapper.updateByEmailAndCode(bean, email, code);
    }

    /**
     * 根据EmailAndCode删除
     */
    @Override
    public Integer deleteEmailCodeByEmailAndCode(String email, String code) {
        return this.emailCodeMapper.deleteByEmailAndCode(email, code);
    }


    private void sendEmailCode(String toEmail, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            //邮件发件人
            helper.setFrom(appConfig.getSendUserName());
            //邮件收件人 1或多个
            helper.setTo(toEmail);

            SysSettingsDto sysSettingsDto = redisComponent.getSysSettingsDto(code);

            //邮件主题
            helper.setSubject(sysSettingsDto.getRegisterEmailTitle());
            //邮件内容
            helper.setText(sysSettingsDto.getRegisterEmailContent());
            //邮件发送时间
            helper.setSentDate(new Date());
            javaMailSender.send(message);
        } catch (Exception e) {
            logger.error("邮件发送失败", e);
            throw new BusinessException("邮件发送失败");
        }
        //整理一下逻辑：
        // 控制层把接收邮箱和用途类型传入下面的sendEmailCode重载方法，在这个方法里根据用途的类型做出不同的判断：如果是注册，判断邮箱是否已经在数据库，否则抛出异常：
        // 然后调用上面的sendEmailCode方法，把接收邮箱和随机生成的验证码传入这个方法里。
        // 在这个方法里，先创建一个构建邮件消息的基本对象message，再创建helper，设置邮件的各种属性。这里设置了true表示这是一个多部分消息，可以包含附件。
        // 然后设置发件人和收件人。发件人的邮箱已经在appconfig里定义了，通过getSendUserName()取出。
        // 然后需要从Redis中获取系统设置信息（主题，内容），这些信息已经在SysSettingsDto类中定义了，通过getSysSettingsDto赋给创建的sysSettingsDto，再从中取出EmailTitle和EmailContent
        // 通过setSentDate方法将邮件的发送日期定义为现在，然后通过send方法发送出去。并做异常捕获。
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String toEmail, Integer type,String ipAddress) {
        //如果是注册，校验邮箱是否已存在
        if (type == Constants.ZERO) {
            UserInfo userInfo = userInfoMapper.selectByEmail(toEmail);
            if (null != userInfo) {
                throw new BusinessException("邮箱已经存在");
            }
        }

        String code = StringTools.getRandomNumber(Constants.LENGTH_5); //生成随机的五位数邮箱验证码
        sendEmailCode(toEmail, code);

        emailCodeMapper.disableEmailCode(toEmail);  //把状态置为1，事实上验证码发出后注册邮箱就已经存在于数据库中了，如果用户点了第二次发送验证码那么数据库中会存在两个一样的邮箱，并且状态码都是0，那么注册成功的时候把状态码置为1会引起冲突
        EmailCode emailCode = new EmailCode();
        emailCode.setCode(code);
        emailCode.setEmail(toEmail);
        emailCode.setStatus(Constants.ZERO);
        emailCode.setCreateTime(new Date());
        emailCode.setIpAddress(ipAddress);
        emailCodeMapper.insert(emailCode);
    }

    @Override
    public void checkCode(String email, String code) {
        EmailCode emailCode = emailCodeMapper.selectByEmailAndCode(email, code);
        if (null == emailCode) {
            throw new BusinessException("邮箱验证码不正确");
        }
        if (emailCode.getStatus() == 1 || System.currentTimeMillis() - emailCode.getCreateTime().getTime() > Constants.LENGTH_15 * 1000 * 60) {
            throw new BusinessException("邮箱验证码已失效");
        }
        emailCodeMapper.disableEmailCode(email);
    }
}