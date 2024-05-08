package com.NanBan.service.impl;

import com.NanBan.entity.constants.Constants;
import com.NanBan.entity.enums.*;
import com.NanBan.entity.po.ForumArticle;
import com.NanBan.entity.po.ForumComment;
import com.NanBan.entity.po.LikeRecord;
import com.NanBan.entity.po.UserMessage;
import com.NanBan.entity.query.*;
import com.NanBan.entity.vo.PaginationResultVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.mappers.ForumArticleMapper;
import com.NanBan.mappers.ForumCommentMapper;
import com.NanBan.mappers.LikeRecordMapper;
import com.NanBan.mappers.UserMessageMapper;
import com.NanBan.service.LikeRecordService;
import com.NanBan.utils.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;


/**
 * 点赞记录 业务接口实现
 */
@Service("likeRecordService")
public class LikeRecordServiceImpl implements LikeRecordService {

    @Resource
    private LikeRecordMapper<LikeRecord, LikeRecordQuery> likeRecordMapper;

    @Resource
    private UserMessageMapper<UserMessage, UserMessageQuery> userMessageMapper;

    @Resource
    private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

    @Resource
    private ForumCommentMapper<ForumComment, ForumCommentQuery>forumCommentMapper;

    /**
     * 根据条件查询列表
     */
    @Override

    public List<LikeRecord> findListByParam(LikeRecordQuery param) {
        return this.likeRecordMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(LikeRecordQuery param) {
        return this.likeRecordMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<LikeRecord> findListByPage(LikeRecordQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<LikeRecord> list = this.findListByParam(param);
        PaginationResultVO<LikeRecord> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(LikeRecord bean) {
        return this.likeRecordMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<LikeRecord> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.likeRecordMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<LikeRecord> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.likeRecordMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(LikeRecord bean, LikeRecordQuery param) {
        StringTools.checkParam(param);
        return this.likeRecordMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(LikeRecordQuery param) {
        StringTools.checkParam(param);
        return this.likeRecordMapper.deleteByParam(param);
    }

    /**
     * 根据OpId获取对象
     */
    @Override
    public LikeRecord getLikeRecordByOpId(Integer opId) {
        return this.likeRecordMapper.selectByOpId(opId);
    }

    /**
     * 根据OpId修改
     */
    @Override
    public Integer updateLikeRecordByOpId(LikeRecord bean, Integer opId) {
        return this.likeRecordMapper.updateByOpId(bean, opId);
    }

    /**
     * 根据OpId删除
     */
    @Override
    public Integer deleteLikeRecordByOpId(Integer opId) {
        return this.likeRecordMapper.deleteByOpId(opId);
    }

    /**
     * 根据ObjectIdAndUserIdAndOpType获取对象
     */
    @Override
    public LikeRecord getLikeRecordByObjectIdAndUserIdAndOpType(String objectId, String userId, Integer opType) {
        return this.likeRecordMapper.selectByObjectIdAndUserIdAndOpType(objectId, userId, opType);
    }

    /**
     * 根据ObjectIdAndUserIdAndOpType修改
     */
    @Override
    public Integer updateLikeRecordByObjectIdAndUserIdAndOpType(LikeRecord bean, String objectId, String userId, Integer opType) {
        return this.likeRecordMapper.updateByObjectIdAndUserIdAndOpType(bean, objectId, userId, opType);
    }

    /**
     * 根据ObjectIdAndUserIdAndOpType删除
     */
    @Override
    public Integer deleteLikeRecordByObjectIdAndUserIdAndOpType(String objectId, String userId, Integer opType) {
        return this.likeRecordMapper.deleteByObjectIdAndUserIdAndOpType(objectId, userId, opType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doLike(String objectId, String userId, String nickName, OperRecordOpTypeEnum opTypeEnum) {
        UserMessage userMessage = new UserMessage();
        userMessage.setCreateTime(new Date());
        switch (opTypeEnum) {
            case ARTICLE_LIKE:
                ForumArticle forumArticle = forumArticleMapper.selectByArticleId(objectId);
                if (forumArticle == null) {
                    throw new BusinessException("文章不存在");
                }
                articleLike(objectId, forumArticle, userId, opTypeEnum);
                userMessage.setArticleId(objectId);
                userMessage.setArticleTitle(forumArticle.getTitle());
                userMessage.setMessageType(MessageTypeEnum.ARTICLE_LIKE.getType());
                userMessage.setCommentId(Constants.ZERO);
                userMessage.setReceivedUserId(forumArticle.getUserId());
                break;
            case COMMENT_LIKE:
                ForumComment forumComment = forumCommentMapper.selectByCommentId(Integer.parseInt(objectId));
                if(forumComment == null){
                    throw new BusinessException("评论不存在");
                }
                commentLike(objectId, forumComment, userId, opTypeEnum);
                forumArticle = forumArticleMapper.selectByArticleId(forumComment.getArticleId());

                userMessage.setArticleId(objectId);
                userMessage.setArticleTitle(forumArticle.getTitle());
                userMessage.setMessageType(MessageTypeEnum.ARTICLE_LIKE.getType());
                userMessage.setCommentId(forumComment.getCommentId());
                userMessage.setReceivedUserId(forumComment.getUserId());
                userMessage.setMessageContent(forumComment.getContent());
                break;
        }
        userMessage.setSendUserId(userId);
        userMessage.setSendNickName(nickName);
        userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
        if (!userId.equals(userMessage.getReceivedUserId())) {
            UserMessage dbInfo = userMessageMapper.selectByArticleIdAndCommentIdAndSendUserIdAndMessageType(userMessage.getArticleId(), userMessage.getCommentId(),
                    userMessage.getSendUserId(), userMessage.getMessageType());
            if (dbInfo == null) {
                userMessageMapper.insert(userMessage);
            }
        }
    }

    private void articleLike(String objectId, ForumArticle forumArticle, String userId, OperRecordOpTypeEnum opTypeEnum) {
        LikeRecord record = this.likeRecordMapper.selectByObjectIdAndUserIdAndOpType(objectId, userId, opTypeEnum.getType());
        Integer changeCount = 0;
        if (record != null) {
            changeCount = -1;
            this.likeRecordMapper.deleteByObjectIdAndUserIdAndOpType(objectId, userId, opTypeEnum.getType());
        } else {
            changeCount = 1;
            LikeRecord likeRecord = new LikeRecord();
            likeRecord.setObjectId(objectId);
            likeRecord.setUserId(userId);
            likeRecord.setOpType(opTypeEnum.getType());
            likeRecord.setCreateTime(new Date());
            likeRecord.setAuthorUserId(forumArticle.getUserId());
            this.likeRecordMapper.insert(likeRecord);
        }
        forumArticleMapper.updateArticleCount(UpdateArticleCountTypeEnum.GOOD_COUNT.getType(), changeCount, objectId);

    }

    private void commentLike(String objectId, ForumComment forumComment, String userId, OperRecordOpTypeEnum opTypeEnum) {
        LikeRecord record = this.likeRecordMapper.selectByObjectIdAndUserIdAndOpType(objectId, userId, opTypeEnum.getType());
        Integer changeCount = 0;
        if (record != null) {
            changeCount = -1;
            this.likeRecordMapper.deleteByObjectIdAndUserIdAndOpType(objectId, userId, opTypeEnum.getType());
        } else {
            changeCount = 1;
            LikeRecord likeRecord = new LikeRecord();
            likeRecord.setObjectId(objectId);
            likeRecord.setUserId(userId);
            likeRecord.setOpType(opTypeEnum.getType());
            likeRecord.setCreateTime(new Date());
            likeRecord.setAuthorUserId(forumComment.getUserId());
            this.likeRecordMapper.insert(likeRecord);
        }
        forumCommentMapper.updateCommentGoodCount(changeCount, Integer.parseInt(objectId));
    }

}