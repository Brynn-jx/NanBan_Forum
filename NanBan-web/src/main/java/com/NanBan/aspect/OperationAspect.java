package com.NanBan.aspect;

import com.NanBan.annotation.GlobalInterceptor;
import com.NanBan.annotation.VerifyParam;
import com.NanBan.entity.constants.Constants;
import com.NanBan.entity.dto.SessionWebUserDto;
import com.NanBan.entity.dto.SysSettingDto;
import com.NanBan.entity.enums.DateTimePatternEnum;
import com.NanBan.entity.enums.ResponseCodeEnum;
import com.NanBan.entity.enums.UserOperFrequencyTypeEnum;
import com.NanBan.entity.query.ForumArticleQuery;
import com.NanBan.entity.query.ForumCommentQuery;
import com.NanBan.entity.query.LikeRecordQuery;
import com.NanBan.entity.vo.ResponseVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.service.ForumArticleService;
import com.NanBan.service.ForumCommentService;
import com.NanBan.service.LikeRecordService;
import com.NanBan.utils.DateUtil;
import com.NanBan.utils.StringTools;
import com.NanBan.utils.SysCacheUtils;
import com.NanBan.utils.VerifyUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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
import java.util.Date;

@Component
@Aspect
public class OperationAspect {
    public static final String[] TYPE_BASE = {"java.lang.String", "java.lang.Integer", "java.lang.Long"};
    private static final Logger logger = LoggerFactory.getLogger(OperationAspect.class);

    @Resource
    private ForumArticleService forumArticleService;

    @Resource
    private ForumCommentService forumCommentService;

    @Resource
    private LikeRecordService likeRecordService;


    @Pointcut("@annotation(com.NanBan.annotation.GlobalInterceptor)")
    private void requestInterceptor() {

    }

    @Around("requestInterceptor()")
    public Object interceptorDo(ProceedingJoinPoint point) {
        try {
            Object target = point.getTarget();
            Object[] arguments = point.getArgs();
            String methodName = point.getSignature().getName();
            Class<?>[] parameterTypes = ((MethodSignature) point.getSignature()).getMethod().getParameterTypes();
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if (interceptor == null) {
                return null;
            }
            // 校验登陆
            if (interceptor.checkLogin()) {
                checkLogin();
            }
            /**
             * 校验参数
             */
            if (interceptor.checkParams()) {
                validateParams(method, arguments);
            }

            /**
             * 校验频次
             */
            this.checkFrequency(interceptor.frequencyType());

            Object pointResult = point.proceed();

            if (pointResult instanceof ResponseVO) {
                ResponseVO responseVO = (ResponseVO) pointResult;
                if (Constants.STATUS_SUCCESS.equals(responseVO.getStatus())) {
                    this.addOpCount(interceptor.frequencyType());
                }
            }

            return pointResult;
        } catch (BusinessException e) {
            logger.error("全局拦截器异常", e);
            throw e;
        } catch (Exception e) {
            logger.error("全局拦截器异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        } catch (Throwable e) {
            logger.error("全局拦截器异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }


    void checkFrequency(UserOperFrequencyTypeEnum typeEnum) {
        if (typeEnum == null || typeEnum == UserOperFrequencyTypeEnum.NO_CHECK) {
            return;
        }
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto webUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);

        String curDate = DateUtil.format(new Date(), DateTimePatternEnum.YYYY_MM_DD.getPattern());
        String sessionKey = Constants.SESSION_KEY_FREQUENCY + curDate + typeEnum.getOperType();
        Integer count = (Integer) session.getAttribute(sessionKey);
        SysSettingDto sysSettingDto = SysCacheUtils.getSysSetting();
        switch (typeEnum) {
            case POST_ARTICLE:
                if(count == null){
                    ForumArticleQuery forumArticleQuery = new ForumArticleQuery();
                    forumArticleQuery.setUserId(webUserDto.getUserId());
                    forumArticleQuery.setPostTimeStart(curDate);
                    forumArticleQuery.setPostTimeEnd(curDate);
                    count = forumArticleService.findCountByParam(forumArticleQuery);
                }
                if(count >= sysSettingDto.getPostSetting().getPostDayCountThreshold()){
                    throw new BusinessException(ResponseCodeEnum.CODE_602);
                }
                break;

            case POST_COMMENT:
                if(count == null){
                    ForumCommentQuery forumCommentQuery = new ForumCommentQuery();
                    forumCommentQuery.setUserId(webUserDto.getUserId());
                    forumCommentQuery.setPostTimeStart(curDate);
                    forumCommentQuery.setPostTimeEnd(curDate);
                    count = forumCommentService.findCountByParam(forumCommentQuery);
                }
                if(count >= sysSettingDto.getCommentSetting().getCommentDayCountThreshold()){
                    throw new BusinessException(ResponseCodeEnum.CODE_602);
                }
                break;

            case DO_LIKE:
                if(count == null){
                    LikeRecordQuery likeRecordQuery = new LikeRecordQuery();
                    likeRecordQuery.setUserId(webUserDto.getUserId());
                    likeRecordQuery.setCreateTimeStart(curDate);
                    likeRecordQuery.setCreateTimeEnd(curDate);
                    count = likeRecordService.findCountByParam(likeRecordQuery);
                }
                if(count >= sysSettingDto.getLikeSetting().getLikeDayCountThreshold()){
                    throw new BusinessException(ResponseCodeEnum.CODE_602);
                }
                break;
            case IMAGE_UPLOAD:
                if(count == null){
                    count = 0;
                }
                if(count >= sysSettingDto.getPostSetting().getPostDayCountThreshold()){
                    throw new BusinessException(ResponseCodeEnum.CODE_602);
                }
        }
        session.setAttribute(sessionKey, count);
    }

    private void addOpCount(UserOperFrequencyTypeEnum typeEnum) {
        if (typeEnum == null || typeEnum == UserOperFrequencyTypeEnum.NO_CHECK) {
            return;
        }
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        String curDate = DateUtil.format(new Date(), DateTimePatternEnum.YYYY_MM_DD.getPattern());
        String sessionKey = Constants.SESSION_KEY_FREQUENCY + curDate + typeEnum.getOperType();
        Integer count = (Integer) session.getAttribute(sessionKey);
        session.setAttribute(sessionKey, count + 1);
    }


    private void checkLogin() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        Object obj = session.getAttribute(Constants.SESSION_KEY);
        if (obj == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
    }

    private void validateParams(Method method, Object[] arguments) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = arguments[i];
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if (verifyParam == null) {
                continue;
            }
            if (ArrayUtils.contains(TYPE_BASE, parameter.getParameterizedType().getTypeName())) {
                checkValue(value, verifyParam);
            } else {

            }
        }
    }

    private void checkObjValue(Parameter parameter, Object value) {
        try {
            String typeName = parameter.getParameterizedType().getTypeName();
            Class clazz = Class.forName(typeName);
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields){
                VerifyParam fieldVerifyParam = field.getAnnotation(VerifyParam.class);
                if(fieldVerifyParam == null){
                    continue;
                }
                field.setAccessible(true);
                Object resultValue = field.get(value);
                checkValue(resultValue, fieldVerifyParam);
            }
        }catch (Exception e){
            logger.error("校验参数失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    private void checkValue(Object value, VerifyParam verifyParam) {
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
        if (!isEmpty && !StringTools.isEmpty(verifyParam.regex().getRegex()) && !VerifyUtils.verify(verifyParam.regex(), String.valueOf(value))) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }
}
