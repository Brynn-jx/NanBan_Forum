package com.NanBan.service.impl;

import com.NanBan.entity.dto.SessionWebUserDto;
import com.NanBan.entity.enums.*;
import com.NanBan.entity.po.*;
import com.NanBan.entity.query.ForumArticleAttachmentDownloadQuery;
import com.NanBan.entity.query.ForumArticleAttachmentQuery;
import com.NanBan.entity.query.SimplePage;
import com.NanBan.entity.query.UserMessageQuery;
import com.NanBan.entity.vo.PaginationResultVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.mappers.ForumArticleAttachmentDownloadMapper;
import com.NanBan.mappers.ForumArticleAttachmentMapper;
import com.NanBan.mappers.UserMessageMapper;
import com.NanBan.service.ForumArticleAttachmentService;
import com.NanBan.service.ForumArticleService;
import com.NanBan.service.UserInfoService;
import com.NanBan.utils.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;


/**
 * 文件信息 业务接口实现
 */
@Service("forumArticleAttachmentService")
public class ForumArticleAttachmentServiceImpl implements ForumArticleAttachmentService {

    @Resource
    private ForumArticleAttachmentMapper<ForumArticleAttachment, ForumArticleAttachmentQuery> forumArticleAttachmentMapper;

    @Resource
    private ForumArticleAttachmentDownloadMapper<ForumArticleAttachmentDownload, ForumArticleAttachmentDownloadQuery> forumArticleAttachmentDownloadMapper;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private ForumArticleService forumArticleService;

    @Resource
    private UserMessageMapper<UserMessage, UserMessageQuery> userMessageMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<ForumArticleAttachment> findListByParam(ForumArticleAttachmentQuery param) {
        return this.forumArticleAttachmentMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(ForumArticleAttachmentQuery param) {
        return this.forumArticleAttachmentMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<ForumArticleAttachment> findListByPage(ForumArticleAttachmentQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<ForumArticleAttachment> list = this.findListByParam(param);
        PaginationResultVO<ForumArticleAttachment> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(ForumArticleAttachment bean) {
        return this.forumArticleAttachmentMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<ForumArticleAttachment> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumArticleAttachmentMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<ForumArticleAttachment> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumArticleAttachmentMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(ForumArticleAttachment bean, ForumArticleAttachmentQuery param) {
        StringTools.checkParam(param);
        return this.forumArticleAttachmentMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(ForumArticleAttachmentQuery param) {
        StringTools.checkParam(param);
        return this.forumArticleAttachmentMapper.deleteByParam(param);
    }

    /**
     * 根据FileId获取对象
     */
    @Override
    public ForumArticleAttachment getForumArticleAttachmentByFileId(String fileId) {
        return this.forumArticleAttachmentMapper.selectByFileId(fileId);
    }

    /**
     * 根据FileId修改
     */
    @Override
    public Integer updateForumArticleAttachmentByFileId(ForumArticleAttachment bean, String fileId) {
        return this.forumArticleAttachmentMapper.updateByFileId(bean, fileId);
    }

    /**
     * 根据FileId删除
     */
    @Override
    public Integer deleteForumArticleAttachmentByFileId(String fileId) {
        return this.forumArticleAttachmentMapper.deleteByFileId(fileId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumArticleAttachment downloadAttachment(String fileId, SessionWebUserDto sessionWebUserDto) {
        ForumArticleAttachment forumArticleAttachment = this.forumArticleAttachmentMapper.selectByFileId(fileId);
        if (forumArticleAttachment == null) {
            throw new BusinessException("附件不存在");
        }
        ForumArticleAttachmentDownload download = null;
        if (forumArticleAttachment.getIntegral() > 0 && !sessionWebUserDto.getUserId().equals(forumArticleAttachment.getUserId())) {
            download = this.forumArticleAttachmentDownloadMapper.selectByFileIdAndUserId(fileId, sessionWebUserDto.getUserId());
            if (download == null) {
                UserInfo userInfo = userInfoService.getUserInfoByUserId(sessionWebUserDto.getUserId());
                if (userInfo.getCurrentIntegral() - forumArticleAttachment.getIntegral() < 0) {
                    throw new BusinessException("积分不够");
                }
            }
        }
        ForumArticleAttachmentDownload updateDownload = new ForumArticleAttachmentDownload();
        updateDownload.setArticleId(forumArticleAttachment.getArticleId());
        updateDownload.setFileId(fileId);
        updateDownload.setUserId(sessionWebUserDto.getUserId());
        updateDownload.setDownloadCount(1);
        this.forumArticleAttachmentDownloadMapper.insertOrUpdate(updateDownload);

        this.forumArticleAttachmentMapper.updateDownloadCount(fileId);

        // 如果是自己或者是已经下载过的就不要在扣积分
        if(sessionWebUserDto.getUserId().equals(forumArticleAttachment.getUserId()) || download != null){
            return forumArticleAttachment;
        }

        // 扣除下载人积分
        userInfoService.updateUserIntegral(sessionWebUserDto.getUserId(), UserIntegralOperTypeEnum.USER_DOWNLOAD_ATTACHMENT,
                UserIntegralChangeTypeEnum.REDUCE.getChangeType(), forumArticleAttachment.getIntegral());

        // 给附件提供者增加积分
        userInfoService.updateUserIntegral(forumArticleAttachment.getUserId(), UserIntegralOperTypeEnum.DOWNLOAD_ATTACHMENT,
                UserIntegralChangeTypeEnum.ADD.getChangeType(), forumArticleAttachment.getIntegral());

        // 记录信息
        ForumArticle forumArticle = forumArticleService.getForumArticleByArticleId(forumArticleAttachment.getArticleId());

        UserMessage userMessage = new UserMessage();
        userMessage.setMessageType(MessageTypeEnum.DOWNLOAD_ATTACHMENT.getType());
        userMessage.setCreateTime(new Date());
        userMessage.setArticleId(forumArticle.getArticleId());
        userMessage.setArticleTitle(forumArticle.getTitle());
        userMessage.setReceivedUserId(forumArticle.getUserId());
        userMessage.setCommentId(0);
        userMessage.setSendUserId(sessionWebUserDto.getUserId());
        userMessage.setSendNickName(sessionWebUserDto.getNickName());
        userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
        userMessageMapper.insert(userMessage);

        return forumArticleAttachment;
    }
}