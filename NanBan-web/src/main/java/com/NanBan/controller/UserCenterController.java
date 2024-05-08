package com.NanBan.controller;

import com.NanBan.annotation.GlobalInterceptor;
import com.NanBan.annotation.VerifyParam;
import com.NanBan.controller.base.ABaseController;
import com.NanBan.entity.dto.SessionWebUserDto;
import com.NanBan.entity.enums.ArticleStatusEnum;
import com.NanBan.entity.enums.MessageTypeEnum;
import com.NanBan.entity.enums.ResponseCodeEnum;
import com.NanBan.entity.enums.UserStatusEnum;
import com.NanBan.entity.po.UserInfo;
import com.NanBan.entity.query.ForumArticleQuery;
import com.NanBan.entity.query.LikeRecordQuery;
import com.NanBan.entity.query.UserIntegralRecordQuery;
import com.NanBan.entity.query.UserMessageQuery;
import com.NanBan.entity.vo.PaginationResultVO;
import com.NanBan.entity.vo.ResponseVO;
import com.NanBan.entity.vo.web.ForumArticleVO;
import com.NanBan.entity.vo.web.UserInfoVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.service.*;
import com.NanBan.utils.CopyTools;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController("userCenterController")
@RequestMapping("/ucenter")
public class UserCenterController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private ForumArticleService forumArticleService;

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    private UserIntegralRecordService userIntegralRecordService;

    @Resource
    private UserMessageService userMessageService;

    @RequestMapping("/getUserInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getUserInfo(@VerifyParam(required = true) String userId) {
        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId);
        if (userInfo == null || UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        ForumArticleQuery articleQuery = new ForumArticleQuery();
        articleQuery.setUserId(userId);
        articleQuery.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        Integer postCount = forumArticleService.findCountByParam(articleQuery);

        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        userInfoVO.setPostCount(postCount);

        LikeRecordQuery recordQuery = new LikeRecordQuery();
        recordQuery.setAuthorUserId(userId);
        Integer likeCount = likeRecordService.findCountByParam(recordQuery);
        userInfoVO.setLikeCount(likeCount);
        return getSuccessResponseVO(userInfoVO);
    }

    @RequestMapping("/loadUserArticle")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadUserArticle(HttpSession session, @VerifyParam(required = true) String userId, @VerifyParam(required = true) Integer type, Integer pageNo) {
        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId);
        if (userInfo == null || UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        ForumArticleQuery articleQuery = new ForumArticleQuery();
        articleQuery.setOrderBy("post_time desc");
        articleQuery.setPageNo(pageNo);
        if (type == 0) {
            articleQuery.setUserId(userId);
        } else if (type == 1) {
            articleQuery.setCommentUserId(userId);
        } else if (type == 2) {
            articleQuery.setLikeUserId(userId);
        }
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if (userDto != null) {
            articleQuery.setCurrentUserId(userDto.getUserId());
        } else {
            articleQuery.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        }
        PaginationResultVO resultVO = forumArticleService.findListByPage(articleQuery);

        return getSuccessResponseVO(convert2PaginationVO(resultVO, ForumArticleVO.class));
    }

    @RequestMapping("/updateUserInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO updateUserInfo(HttpSession session, Integer sex, @VerifyParam(max = 100) String personDescription, MultipartFile avatar) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userDto.getUserId());
        userInfo.setSex(sex);
        userInfo.setPersonDescription(personDescription);
        userInfoService.updateUserInfo(userInfo, avatar);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadUserIntegralRecord")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadUserIntegralRecord(HttpSession session, Integer pageNo, String createTimeStart, String createTimeEnd) {
        UserIntegralRecordQuery recordQuery = new UserIntegralRecordQuery();
        recordQuery.setUserId(getUserInfoFromSession(session).getUserId());
        recordQuery.setPageNo(pageNo);
        recordQuery.setCreateTimeStart(createTimeStart);
        recordQuery.setCreateTimeEnd(createTimeEnd);
        recordQuery.setOrderBy("record_id desc");
        PaginationResultVO resultVO = userIntegralRecordService.findListByPage(recordQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/getMessageCount")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getMessageCount(HttpSession session) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        return getSuccessResponseVO(userMessageService.getUserMessageCount(userDto.getUserId()));
    }

    @RequestMapping("/loadMessageList")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public ResponseVO loadMessageList(HttpSession session, @VerifyParam(required = true) String code, Integer pageNo) {
        MessageTypeEnum typeEnum = MessageTypeEnum.getByCode(code);
        if(typeEnum == null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        SessionWebUserDto userDto = getUserInfoFromSession(session);

        UserMessageQuery query = new UserMessageQuery();
        query.setPageNo(pageNo);
        query.setReceivedUserId(userDto.getUserId());
        query.setMessageType(typeEnum.getType());
        query.setOrderBy("message_id desc");
        PaginationResultVO resultVO = userMessageService.findListByPage(query);
        if(pageNo == null || pageNo == 1){
            userMessageService.readMessageByType(userDto.getUserId(), typeEnum.getType());
        }
        return getSuccessResponseVO(resultVO);
    }
}
