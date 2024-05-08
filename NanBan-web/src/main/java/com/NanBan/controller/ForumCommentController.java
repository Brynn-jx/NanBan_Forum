package com.NanBan.controller;

import com.NanBan.annotation.GlobalInterceptor;
import com.NanBan.annotation.VerifyParam;
import com.NanBan.controller.base.ABaseController;
import com.NanBan.entity.constants.Constants;
import com.NanBan.entity.dto.SessionWebUserDto;
import com.NanBan.entity.enums.*;
import com.NanBan.entity.po.ForumComment;
import com.NanBan.entity.po.LikeRecord;
import com.NanBan.entity.query.ForumCommentQuery;
import com.NanBan.entity.vo.ResponseVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.service.ForumCommentService;
import com.NanBan.service.LikeRecordService;
import com.NanBan.utils.StringTools;
import com.NanBan.utils.SysCacheUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;


@RestController
@RequestMapping("/comment")
public class ForumCommentController extends ABaseController {

    @Resource
    private ForumCommentService forumCommentService;

    @Resource
    private LikeRecordService likeRecordService;

    @RequestMapping("/loadComment")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadComment(HttpSession session,
                                  @VerifyParam(required = true) String articleId,
                                  Integer pageNo,
                                  Integer orderType) {
        final String ORDER_TYPE0 = "good_count desc,comment_id asc";
        final String ORDER_TYPE1 = "comment_id desc";

        if (!SysCacheUtils.getSysSetting().getCommentSetting().getCommentOpen()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        ForumCommentQuery commentQuery = new ForumCommentQuery();
        commentQuery.setArticleId(articleId);
        String orderBy = orderType == null || orderType == Constants.ZERO ? ORDER_TYPE0 : ORDER_TYPE1;
        commentQuery.setOrderBy("top_type desc," + orderBy);
        commentQuery.setPageNo(pageNo);

        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if (userDto != null) {
            commentQuery.setQueryLikeType(true);
            commentQuery.setCurrentUserId(userDto.getUserId());
        } else {
            commentQuery.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        }
        commentQuery.setPageNo(pageNo);
        commentQuery.setPageSize(PageSize.SIZE50.getSize());
        commentQuery.setpCommentId(0);
        commentQuery.setLoadChildren(true);
        return getSuccessResponseVO(forumCommentService.findListByPage(commentQuery));
    }

    @RequestMapping("/doLike")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public ResponseVO doLike(HttpSession session,
                             @VerifyParam(required = true) Integer commentId) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        String objectId = String.valueOf(commentId);
        likeRecordService.doLike(objectId, userDto.getUserId(), userDto.getNickName(), OperRecordOpTypeEnum.COMMENT_LIKE);
        LikeRecord likeRecord =
                likeRecordService.getLikeRecordByObjectIdAndUserIdAndOpType(objectId, userDto.getUserId(), OperRecordOpTypeEnum.COMMENT_LIKE.getType());
        ForumComment comment = forumCommentService.getForumCommentByCommentId(commentId);
        comment.setLikeType(likeRecord == null ? 0 : 1);
        return getSuccessResponseVO(comment);
    }

    @RequestMapping("/changeTopType")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public ResponseVO changeTopType(HttpSession session,
                                    @VerifyParam(required = true) Integer commentId,
                                    @VerifyParam(required = true) Integer topType) {
        forumCommentService.changeTopType(getUserInfoFromSession(session).getUserId(), commentId, topType);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/postComment")
    @GlobalInterceptor(checkParams = true, checkLogin = true, frequencyType = UserOperFrequencyTypeEnum.POST_COMMENT)
    public ResponseVO postComment(HttpSession session,
                                  @VerifyParam(required = true) String articleId,
                                  @VerifyParam(required = true) Integer pCommentId,
                                  @VerifyParam(min = 5, max = 800) String content,
                                  MultipartFile image,
                                  String replyUserId) {
        if (!SysCacheUtils.getSysSetting().getCommentSetting().getCommentOpen()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if(image == null && StringTools.isEmpty(content)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        SessionWebUserDto userDto = getUserInfoFromSession(session);

        content = StringTools.escapeHtml(content);
        ForumComment comment = new ForumComment();
        comment.setUserId(userDto.getUserId());
        comment.setNickName(userDto.getNickName());
        comment.setUserIpAddress(userDto.getProvince());
        comment.setpCommentId(pCommentId);
        comment.setArticleId(articleId);
        comment.setContent(content);
        comment.setReplyUserId(replyUserId);
        comment.setTopType(CommentTopTypeEnum.NO_TOP.getType());

        forumCommentService.postComment(comment, image);
        if(pCommentId != 0){
            ForumCommentQuery forumCommentQuery = new ForumCommentQuery();
            forumCommentQuery.setArticleId(articleId);
            forumCommentQuery.setCommentId(pCommentId);
            forumCommentQuery.setOrderBy("comment_id asc");
            List<ForumComment> children = forumCommentService.findListByParam(forumCommentQuery);
            return getSuccessResponseVO(children);
        }
        return getSuccessResponseVO(comment);
    }

}
