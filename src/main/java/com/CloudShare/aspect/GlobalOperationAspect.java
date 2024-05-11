package com.CloudShare.aspect;

import com.CloudShare.annotation.GlobalInterceptor;
import com.CloudShare.annotation.VerifyParam;
import com.CloudShare.entity.config.AppConfig;
import com.CloudShare.entity.constants.Constants;
import com.CloudShare.entity.dto.SessionWebUserDto;
import com.CloudShare.entity.enums.ResponseCodeEnum;
import com.CloudShare.entity.pojo.UserInfo;
import com.CloudShare.entity.query.UserInfoQuery;
import com.CloudShare.exception.BusinessException;
import com.CloudShare.service.UserInfoService;
import com.CloudShare.utils.StringTools;
import com.CloudShare.utils.VerifyUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Deque;
import java.util.List;

// 本项目的AOP编程实现过程：
// 1、引入aop的包
// 2、定义一个切点（这里的切点是被@GlobalInterceptor注解所定义的方法）
// 3、使用@Before注解在切点执行前执行被@Before注解的方法也就是interceptorDo

@Component("operationAspect")  //，不属于spring三大层，交给spring管理，加入到IOC容器中
@Aspect  //标明切面
public class GlobalOperationAspect {

    private static Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);
    private static final String TYPE_STRING = "java.lang.String";
    private static final String TYPE_INTEGER = "java.lang.Integer";
    private static final String TYPE_LONG = "java.lang.Long";

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private AppConfig appConfig;


    //切点是通过@Pointcut注解定义的，匹配带有@GlobalInterceptor注解的方法。
    @Pointcut("@annotation(com.CloudShare.annotation.GlobalInterceptor)")
    private void requestInterceptor() {
    }

    //为什么需要定义一个空方法requestInterceptor？我们需要@Pointcut定义切点，而注解一般是定义在方法上的，所以我们需要一个方法去引入@Pointcut这个注解来定义切点
    @Before("requestInterceptor()")  //切点注解@Pointcut定义在requestInterceptor()方法上，@before注解里的参数设置为requestInterceptor()是为了引入切点，并在切点执行之前执行以下部分内容
    public void interceptorDo(JoinPoint point) throws BusinessException {
        try {
            Object target = point.getTarget();  //获取拦截的目标对象，也就是被拦截方法所属的对象
            Object[] arguments = point.getArgs();  //获取被拦截方法的参数值
            String methodName = point.getSignature().getName();  //获取被拦截方法的名称
            Class<?>[] parameterTypes = ((MethodSignature) point.getSignature()).getMethod().getParameterTypes();  //获取被拦截方法的参数类型
            Method method = target.getClass().getMethod(methodName, parameterTypes);  //通过反射获取被拦截方法的 Method 对象
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);  //获取被拦截方法上的 GlobalInterceptor 注解
            if (null == interceptor) {
                return;
            }
            /**
             * 校验登录
             */
            if (interceptor.checkLogin() || interceptor.checkAdmin()) {
                checkLogin(interceptor.checkAdmin());
            }
            /**
             * 校验参数
             */
            if (interceptor.checkParams()) {
                validateParams(method, arguments);

            }
        } catch (BusinessException e) {  //业务异常
            logger.error("全局拦截器-业务异常", e);
            throw e;
        } catch (Exception e) {   //捕获通用的 Exception 异常
            logger.error("全局拦截器-Exception异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        } catch (Throwable e) {   //捕获更加通用的 Throwable 异常，包括 Error 类和其他非受检异常，如果捕获到，则记录异常信息，并抛出新的 BusinessException 异常。
            logger.error("全局拦截器-Throwable异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }


    //校验登录
    private void checkLogin(Boolean checkAdmin) {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto sessionUser = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        //没有登录信息  且  不处于开发环境
        if (sessionUser == null && appConfig.getDev() != null && appConfig.getDev()) {
            List<UserInfo> userInfoList = userInfoService.findListByParam(new UserInfoQuery());
            if (!userInfoList.isEmpty()) {
                UserInfo userInfo = userInfoList.get(0);
                sessionUser = new SessionWebUserDto();
                sessionUser.setUserId(userInfo.getUserId());
                sessionUser.setNickName(userInfo.getNickName());
                sessionUser.setAdmin(true);
                session.setAttribute(Constants.SESSION_KEY, sessionUser);
            }
        }
        if (null == sessionUser) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        if (checkAdmin && !sessionUser.getAdmin()){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }

    private void validateParams(Method m, Object[] arguments) throws BusinessException {

        Parameter[] parameters = m.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = arguments[i];  //获取方法调用时传递的参数值
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);  //从当前参数上获取 @VerifyParam 注解
//            System.out.println(verifyParam+"====verifyparams");  //输出样例：@com.CloudShare.annotation.VerifyParam(regex=NO, min=1, max=1, required=true)====verifyparams
            if (verifyParam == null) {
                continue;
            }
            //校验基本数据类型
            if (TYPE_STRING.equals(parameter.getParameterizedType().getTypeName()) ||
                    TYPE_LONG.equals(parameter.getParameterizedType().getTypeName()) ||
                    TYPE_INTEGER.equals(parameter.getParameterizedType().getTypeName())) {
                checkValue(value, verifyParam);
                //如果传递的是对象
            } else {
                checkObjValue(parameter, value);
            }
        }
    }

    private void checkObjValue(Parameter parameter, Object value) {
        try {
            String typeName = parameter.getParameterizedType().getTypeName();
            Class classz = Class.forName(typeName);
            Field[] fields = classz.getDeclaredFields();
            for (Field field : fields) {
                VerifyParam fieldVerifyParam = field.getAnnotation(VerifyParam.class);  //拿到VerifyParam里的内容
                if (fieldVerifyParam == null) {
                    continue;
                }
                field.setAccessible(true);
                Object resultValue = field.get(value);
                checkValue(resultValue, fieldVerifyParam);
            }
        } catch (BusinessException e) {
            logger.error("校验参数失败1", e);
            throw e;
        } catch (Exception e) {
            logger.error("校验参数失败2", e);
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    /**
     * 校验参数
     *
     * @param value
     * @param verifyParam
     * @throws BusinessException
     */
    private void checkValue(Object value, VerifyParam verifyParam) throws BusinessException {
        Boolean isEmpty = value == null || StringTools.isEmpty(value.toString());
        Integer length = value == null ? 0 : value.toString().length();

        /**
         * 校验空
         */
        if (isEmpty && verifyParam.required()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }


        /**
         * 校验长度
         */
        if (!isEmpty && (verifyParam.max() != -1 && verifyParam.max() < length || verifyParam.min() != -1 && verifyParam.min() > length)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        /**
         * 校验正则
         */
        //VerifyUtils.verify是真正执行正则的地方
        if (!isEmpty && !StringTools.isEmpty(verifyParam.regex().getRegex()) && !VerifyUtils.verify(verifyParam.regex(), String.valueOf(value))) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }
}