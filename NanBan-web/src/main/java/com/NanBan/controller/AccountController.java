package com.NanBan.controller;

import com.NanBan.annotation.GlobalInterceptor;
import com.NanBan.annotation.VerifyParam;
import com.NanBan.controller.base.ABaseController;
import com.NanBan.entity.constants.Constants;
import com.NanBan.entity.dto.CreateImageCode;
import com.NanBan.entity.dto.SessionWebUserDto;
import com.NanBan.entity.dto.SysSetting4CommentDto;
import com.NanBan.entity.dto.SysSettingDto;
import com.NanBan.entity.enums.VerifyRegexEnum;
import com.NanBan.entity.vo.ResponseVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.service.EmailCodeService;
import com.NanBan.service.UserInfoService;
import com.NanBan.utils.SysCacheUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//每个方法的返回值都会以 JSON 或 XML 的形式直接写入 HTTP 响应体中，相当于在每个方法上都添加了 @ResponseBody 注解。
@RestController
public class AccountController extends ABaseController {
    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private UserInfoService userInfoService;

    /**
     * @param response
     * @param session
     * @param type     区分是注册还是登陆
     * @throws IOException
     */
    // session是一个存储在WEB服务器端的java对象，该对象代表用户和WEB服务器的一次会话(远程访问的网络页面的服务器)。Cookie可以将会话状态保存在浏览器客户端(本地电脑上的浏览器)
    /*
     1. 打开浏览器，在浏览器上发送首次请求
     2. 服务器会创建一个HttpSession对象，该对象代表一次会话
     3. 同时生成HttpSession对象对应的Cookie对象，并且Cookie对象的name是jsessionid，Cookie的value是32位长度的字符串（jsessionid=xxxx）
     4. 服务器将Cookie的value和HttpSession对象绑定到session列表中
     5. 服务器将Cookie完整发送给浏览器客户端
     6. 浏览器客户端将Cookie保存到缓存中
     7. 只要浏览器不关闭，Cookie就不会消失
     8. 当再次发送请求的时候，会自动提交缓存中当的Cookie
     9. 服务器接收到Cookie，验证该Cookie的name是否是jsessionid，然后获取该Cookie的value
     10. 通过Cookie的value去session列表中检索对应的HttpSession对象

    * */
    //用于将任意HTTP 请求映射到控制器方法上
    @RequestMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        // 设置页面不缓存
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        // 登陆注册
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            // 获取邮箱
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        // 通过这个方法可以拿到一个字节流，然后可以向Response容器中写入字节数据，最后客户机向Response容器中拿去数据进行显示
        vCode.write(response.getOutputStream());
    }

    @RequestMapping("/sendEmailCode")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO sendEmailCode(HttpSession session,
                                    @VerifyParam(required = true) String email,
                                    @VerifyParam(required = true) String checkCode,
                                    @VerifyParam(required = true) Integer type) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                throw new BusinessException("图片验证码错误");
            }
            emailCodeService.sendEmailCode(email, type);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }


    @RequestMapping("/register")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO register(HttpSession session,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                               @VerifyParam(required = true) String emailCode,
                               @VerifyParam(required = true, max = 20) String nickName,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
                               @VerifyParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码错误");
            }
            userInfoService.register(email, emailCode, nickName, password);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/login")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO login(HttpSession session, HttpServletRequest request,
                            @VerifyParam(required = true) String email,
                            @VerifyParam(required = true) String password,
                            @VerifyParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码错误");
            }
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email, password, getIpAddr(request));
            session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
            return getSuccessResponseVO(sessionWebUserDto);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/getUserInfo")
    @GlobalInterceptor()
    public ResponseVO getUserInfo(HttpSession session) {
        return getSuccessResponseVO(getUserInfoFromSession(session));
    }

    @RequestMapping("/logout")
    @GlobalInterceptor()
    public ResponseVO logout(HttpSession session) {
        session.invalidate();
        return getSuccessResponseVO(null);
    }

    /**
     * 获取系统设置，下面是获取评论是否开启
     *
     * @return
     */
    @RequestMapping("/getSysSetting")
    @GlobalInterceptor()
    public ResponseVO getSysSetting() {
        SysSettingDto settingDto = SysCacheUtils.getSysSetting();
        SysSetting4CommentDto commentDto = settingDto.getCommentSetting();
        Map<String, Object> result = new HashMap<>();
        result.put("commentOpen", commentDto.getCommentOpen());
        return getSuccessResponseVO(result);
    }


    @RequestMapping("/resetPwd")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO resetPwd(HttpSession session,
                               @VerifyParam(required = true) String email,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
                               @VerifyParam(required = true) String emailCode,
                               @VerifyParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码错误");
            }
            userInfoService.resetPwd(email,password,emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }
}
