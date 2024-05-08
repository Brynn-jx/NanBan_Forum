package com.NanBan.service.impl;

import com.NanBan.entity.constants.Constants;
import com.NanBan.entity.dto.FileUploadDto;
import com.NanBan.entity.enums.*;
import com.NanBan.entity.po.ForumArticle;
import com.NanBan.entity.po.ForumComment;
import com.NanBan.entity.po.UserInfo;
import com.NanBan.entity.po.UserMessage;
import com.NanBan.entity.query.ForumArticleQuery;
import com.NanBan.entity.query.ForumCommentQuery;
import com.NanBan.entity.query.SimplePage;
import com.NanBan.entity.vo.PaginationResultVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.mappers.ForumArticleMapper;
import com.NanBan.mappers.ForumCommentMapper;
import com.NanBan.service.ForumCommentService;
import com.NanBan.service.UserInfoService;
import com.NanBan.service.UserMessageService;
import com.NanBan.utils.FileUtils;
import com.NanBan.utils.StringTools;
import com.NanBan.utils.SysCacheUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 评论 业务接口实现
 */
@Service("forumCommentService")
public class ForumCommentServiceImpl implements ForumCommentService {

    @Resource
    private ForumCommentMapper<ForumComment, ForumCommentQuery> forumCommentMapper;

    @Resource
    private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private UserMessageService userMessageService;

    @Resource
    private FileUtils fileUtils;


    /**
     * 使用注解内部调用 下面的auditComment调用auditCommentSingle直接使用this.forumCommentMapper会使得事物失效
     * 必须重新定义
     * 加上Lazy防止循环依赖
     */
    @Lazy
    @Resource
    private ForumCommentService forumCommentService;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<ForumComment> findListByParam(ForumCommentQuery param) {

        List<ForumComment> list = this.forumCommentMapper.selectList(param);
        // 获取二级评论
        if (param.getLoadChildren() != null && param.getLoadChildren()) {
            ForumCommentQuery subQuery = new ForumCommentQuery();
            // 评论的类型，最新或者是最热
            subQuery.setQueryLikeType(param.getQueryLikeType());
            subQuery.setCurrentUserId(param.getCurrentUserId());
            subQuery.setArticleId(param.getArticleId());
            subQuery.setStatus(param.getStatus());
            List<Integer> pcommentIdList = list.stream().map(ForumComment::getCommentId).distinct().collect(Collectors.toList());
            subQuery.setPcomentIdList(pcommentIdList);
            List<ForumComment> subCommentList = this.forumCommentMapper.selectList(subQuery);

            Map<Integer, List<ForumComment>> tempMap = subCommentList.stream().collect(Collectors.groupingBy(ForumComment::getpCommentId));
            list.forEach(item -> {
                item.setChildren(tempMap.get(item.getCommentId()));
            });
        }
        return list;
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(ForumCommentQuery param) {
        return this.forumCommentMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<ForumComment> findListByPage(ForumCommentQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<ForumComment> list = this.findListByParam(param);
        PaginationResultVO<ForumComment> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(ForumComment bean) {
        return this.forumCommentMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<ForumComment> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumCommentMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<ForumComment> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumCommentMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(ForumComment bean, ForumCommentQuery param) {
        StringTools.checkParam(param);
        return this.forumCommentMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(ForumCommentQuery param) {
        StringTools.checkParam(param);
        return this.forumCommentMapper.deleteByParam(param);
    }

    /**
     * 根据CommentId获取对象
     */
    @Override
    public ForumComment getForumCommentByCommentId(Integer commentId) {
        return this.forumCommentMapper.selectByCommentId(commentId);
    }

    /**
     * 根据CommentId修改
     */
    @Override
    public Integer updateForumCommentByCommentId(ForumComment bean, Integer commentId) {
        return this.forumCommentMapper.updateByCommentId(bean, commentId);
    }

    /**
     * 根据CommentId删除
     */
    @Override
    public Integer deleteForumCommentByCommentId(Integer commentId) {
        return this.forumCommentMapper.deleteByCommentId(commentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeTopType(String userId, Integer commentId, Integer topType) {
        CommentTopTypeEnum topTypeEnum = CommentTopTypeEnum.getByType(topType);
        if (topTypeEnum == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        ForumComment forumComment = forumCommentMapper.selectByCommentId(commentId);
        if (forumComment == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        ForumArticle forumArticle = forumArticleMapper.selectByArticleId(forumComment.getArticleId());
        if (forumArticle == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (!forumArticle.getUserId().equals(userId) || forumComment.getpCommentId() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (forumComment.getTopType().equals(topType)) {
            return;
        }
        if (CommentTopTypeEnum.TOP.getType().equals(topType)) {
            forumCommentMapper.updateTopTypeByArticleId(forumComment.getArticleId());
        }
        ForumComment updateInfo = new ForumComment();
        updateInfo.setTopType(topType);
        forumCommentMapper.updateByCommentId(updateInfo, forumComment.getCommentId());
    }

    @Override
    public void postComment(ForumComment comment, MultipartFile image) {
        ForumArticle forumArticle = forumArticleMapper.selectByArticleId(comment.getArticleId());
        if (forumArticle == null || !ArticleStatusEnum.AUDIT.getStatus().equals(forumArticle.getStatus())) {
            throw new BusinessException("评论的文章不存在");
        }
        ForumComment pComment = null;
        if (comment.getpCommentId() != 0) {
            pComment = forumCommentMapper.selectByCommentId(comment.getpCommentId());
            if (pComment == null) {
                throw new BusinessException("回复的评论不存在");
            }
        }
        // 判断回复的用户是否存在
        if (!StringTools.isEmpty(comment.getReplyUserId())) {
            UserInfo userInfo = userInfoService.getUserInfoByUserId(comment.getReplyUserId());
            if (userInfo == null) {
                throw new BusinessException("回复的用户不存在");
            }
            comment.setReplyNickName(userInfo.getNickName());
        }
        comment.setPostTime(new Date());
        if (image != null) {
            FileUploadDto uploadDto = fileUtils.uploadFile2Local(image, Constants.FILE_FOLDER_IMAGE, FileUploadTypeEnum.COMMENT_IMAGE);
            comment.setImgPath(uploadDto.getLocalPath());
        }

        Boolean needAudit = SysCacheUtils.getSysSetting().getAuditSetting().getCommentAudit();

        comment.setStatus(needAudit ? CommentStatusEnum.NO_AUDIT.getStatus() : CommentStatusEnum.AUDIT.getStatus());
        this.forumCommentMapper.insert(comment);
        if (needAudit) {
            return;
        }
        updateCommentInfo(comment, forumArticle, pComment);
    }

    public void updateCommentInfo(ForumComment comment, ForumArticle forumArticle, ForumComment pComment) {
        Integer commentIntegral = SysCacheUtils.getSysSetting().getCommentSetting().getCommentIntegral();
        if (commentIntegral > 0) {
            this.userInfoService.updateUserIntegral(comment.getUserId(), UserIntegralOperTypeEnum.POST_COMMENT, UserIntegralChangeTypeEnum.ADD.getChangeType(),
                    commentIntegral);
        }

        if (comment.getpCommentId() == 0) {
            this.forumArticleMapper.updateArticleCount(UpdateArticleCountTypeEnum.COMMENT_COUNT.getType(), Constants.ONE, comment.getArticleId());
        }

        // 记录消息
        UserMessage userMessage = new UserMessage();
        userMessage.setMessageType(MessageTypeEnum.COMMENT.getType());
        userMessage.setCreateTime(new Date());
        userMessage.setArticleId(comment.getArticleId());
        userMessage.setCommentId(comment.getCommentId());
        userMessage.setSendUserId(comment.getUserId());
        userMessage.setSendNickName(comment.getNickName());
        userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
        userMessage.setArticleTitle(forumArticle.getTitle());
        if (comment.getpCommentId() == 0) {
            userMessage.setReceivedUserId(forumArticle.getUserId());
        } else if (comment.getpCommentId() != 0 && StringTools.isEmpty(comment.getReplyUserId())) {
            userMessage.setReceivedUserId(pComment.getUserId());
        } else if (comment.getpCommentId() != 0 && !StringTools.isEmpty(comment.getReplyUserId())) {
            userMessage.setReceivedUserId(comment.getReplyUserId());
        }
        // 不是自己回复自己，就要进行发送信息处理
        if (!comment.getUserId().equals(userMessage.getReceivedUserId())) {
            userMessageService.add(userMessage);
        }
    }

    @Override
    public void delComment(String commentIds) {
        String[] commentIdArray = commentIds.split(",");
        for (String commentId : commentIdArray) {
            forumCommentService.delCommentSingle(Integer.parseInt(commentId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delCommentSingle(Integer commentId) {
        ForumComment comment = forumCommentMapper.selectByCommentId(commentId);
        if (comment == null || CommentStatusEnum.DEL.getStatus().equals(comment.getStatus())) {
            return;
        }
        ForumComment forumComment = new ForumComment();
        forumComment.setStatus(CommentStatusEnum.DEL.getStatus());
        forumCommentMapper.updateByCommentId(forumComment, commentId);

        // 删除已经审核的评论，更新评论数量
        if (CommentStatusEnum.AUDIT.getStatus().equals(comment.getStatus())) {
            if (comment.getCommentId() == 0) {
                forumArticleMapper.updateArticleCount(UpdateArticleCountTypeEnum.COMMENT_COUNT.getType(), -1, comment.getArticleId());
            }
            Integer integer = SysCacheUtils.getSysSetting().getCommentSetting().getCommentIntegral();
            userInfoService.updateUserIntegral(comment.getUserId(), UserIntegralOperTypeEnum.DEL_COMMENT, UserIntegralChangeTypeEnum.REDUCE.getChangeType(), integer);
        }

        UserMessage userMessage = new UserMessage();
        userMessage.setReceivedUserId(comment.getUserId());
        userMessage.setMessageType(MessageTypeEnum.SYS.getType());
        userMessage.setCreateTime(new Date());
        userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
        userMessage.setMessageContent("评论【" + comment.getContent() + "】被管理员删除");
        userMessageService.add(userMessage);
    }

    @Override
    public void auditComment(String commentIds) {
        String[] commentIdArray = commentIds.split(",");
        for (String commentId : commentIdArray) {
            forumCommentService.auditCommentSingle(Integer.parseInt(commentId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditCommentSingle(Integer commentId) {
        ForumComment comment = forumCommentMapper.selectByCommentId(commentId);
        if (!CommentStatusEnum.NO_AUDIT.getStatus().equals(comment.getStatus())) {
            return;
        }
        ForumComment forumComment = new ForumComment();
        forumComment.setStatus(CommentStatusEnum.AUDIT.getStatus());
        forumCommentMapper.updateByCommentId(forumComment, commentId);
        ForumArticle forumArticle = forumArticleMapper.selectByArticleId(comment.getArticleId());
        ForumComment pComment = null;
        if (comment.getpCommentId() != 0 && StringTools.isEmpty(comment.getReplyUserId())) {
            pComment = forumCommentMapper.selectByCommentId(comment.getpCommentId());
        }
        updateCommentInfo(comment, forumArticle, pComment);
    }
}