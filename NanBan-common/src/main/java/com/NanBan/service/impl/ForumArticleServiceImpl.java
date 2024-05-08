package com.NanBan.service.impl;

import com.NanBan.entity.config.AppConfig;
import com.NanBan.entity.constants.Constants;
import com.NanBan.entity.dto.FileUploadDto;
import com.NanBan.entity.dto.SysSetting4AuditDto;
import com.NanBan.entity.enums.*;
import com.NanBan.entity.po.ForumArticle;
import com.NanBan.entity.po.ForumArticleAttachment;
import com.NanBan.entity.po.ForumBoard;
import com.NanBan.entity.po.UserMessage;
import com.NanBan.entity.query.ForumArticleAttachmentQuery;
import com.NanBan.entity.query.ForumArticleQuery;
import com.NanBan.entity.query.SimplePage;
import com.NanBan.entity.vo.PaginationResultVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.mappers.ForumArticleAttachmentMapper;
import com.NanBan.mappers.ForumArticleMapper;
import com.NanBan.service.ForumArticleService;
import com.NanBan.service.ForumBoardService;
import com.NanBan.service.UserInfoService;
import com.NanBan.service.UserMessageService;
import com.NanBan.utils.FileUtils;
import com.NanBan.utils.ImageUtils;
import com.NanBan.utils.StringTools;
import com.NanBan.utils.SysCacheUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;


/**
 * 文章信息 业务接口实现
 */
@Service("forumArticleService")
public class ForumArticleServiceImpl implements ForumArticleService {

    @Resource
    private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

    @Resource
    private ForumBoardService forumBoardService;

    @Resource
    private FileUtils fileUtils;

    @Resource
    private ForumArticleAttachmentMapper<ForumArticleAttachment, ForumArticleAttachmentQuery> forumArticleAttachmentMapper;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private ImageUtils imageUtils;

    @Resource
    private AppConfig appConfig;

    @Lazy // 自己调自己 使用lazy解决循环依赖的问题
    @Resource
    private ForumArticleService forumArticleService;

    @Resource
    private UserMessageService userMessageService;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<ForumArticle> findListByParam(ForumArticleQuery param) {
        return this.forumArticleMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(ForumArticleQuery param) {
        return this.forumArticleMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<ForumArticle> findListByPage(ForumArticleQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<ForumArticle> list = this.findListByParam(param);
        PaginationResultVO<ForumArticle> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(ForumArticle bean) {
        return this.forumArticleMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<ForumArticle> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumArticleMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<ForumArticle> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumArticleMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(ForumArticle bean, ForumArticleQuery param) {
        StringTools.checkParam(param);
        return this.forumArticleMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(ForumArticleQuery param) {
        StringTools.checkParam(param);
        return this.forumArticleMapper.deleteByParam(param);
    }

    /**
     * 根据ArticleId获取对象
     */
    @Override
    public ForumArticle getForumArticleByArticleId(String articleId) {
        return this.forumArticleMapper.selectByArticleId(articleId);
    }

    /**
     * 根据ArticleId修改
     */
    @Override
    public Integer updateForumArticleByArticleId(ForumArticle bean, String articleId) {
        return this.forumArticleMapper.updateByArticleId(bean, articleId);
    }

    /**
     * 根据ArticleId删除
     */
    @Override
    public Integer deleteForumArticleByArticleId(String articleId) {
        return this.forumArticleMapper.deleteByArticleId(articleId);
    }

    /**
     * 获取文章内容并且更新信息，阅读量
     *
     * @param articleId
     * @return
     */
    @Override
    public ForumArticle readArticle(String articleId) {
        ForumArticle forumArticle = this.forumArticleMapper.selectByArticleId(articleId);
        if (forumArticle == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        if (ArticleStatusEnum.AUDIT.getStatus().equals(forumArticle.getStatus())) {
            this.forumArticleMapper.updateArticleCount(UpdateArticleCountTypeEnum.READ_COUNT.getType(), Constants.ONE, articleId);
        }
        return forumArticle;
    }

    @Override
    public void postArticle(Boolean isAdmin, ForumArticle article, ForumArticleAttachment articleAttachment, MultipartFile cover, MultipartFile attachment) {
        resetBoardInfo(isAdmin, article);
        Date curData = new Date();
        String articleId = StringTools.getRandomString(Constants.LENGTH_15);
        article.setArticleId(articleId);
        article.setPostTime(curData);
        article.setLastUpdateTime(curData);

        if (cover != null) {
            FileUploadDto fileUploadDto = fileUtils.uploadFile2Local(cover, Constants.FILE_FOLDER_IMAGE, FileUploadTypeEnum.ARTICLE_COVER);
            article.setCover(fileUploadDto.getLocalPath());
        }

        if (attachment != null) {
            uploadAttachment(article, articleAttachment, attachment, false);
            article.setAttachmentType(ArticleAttachmentTypeEnum.HAVE_ATTACHMENT.getType());
        } else {
            article.setAttachmentType(ArticleAttachmentTypeEnum.NO_ATTACHMENT.getType());
        }

        // 文章审核信息
        if (isAdmin) {
            article.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        } else {
            SysSetting4AuditDto auditDto = SysCacheUtils.getSysSetting().getAuditSetting();
            article.setStatus(auditDto.getPostAudit() ? ArticleStatusEnum.NO_AUDIT.getStatus() : ArticleStatusEnum.AUDIT.getStatus());
        }

        // 发布文章时图片 在未发布前都是存放在本地temp，只有发布了，才上传服务器
        // 替换图片ta
        String content = article.getContent();
        if (!StringTools.isEmpty(content)) {
            String month = imageUtils.resetImageHtml(content);
            String replaceMonth = "/" + month + "/";
            content = content.replace(Constants.FILE_FOLDER_TEMP, replaceMonth);
            article.setContent(content);
            String markdownContent = article.getMarkdownContent();
            if (!StringTools.isEmpty(markdownContent)) {
                markdownContent = markdownContent.replace(Constants.FILE_FOLDER_TEMP, replaceMonth);
                article.setMarkdownContent(markdownContent);
            }
        }
        this.forumArticleMapper.insert(article);

        // 增加积分
        Integer postIntegral = SysCacheUtils.getSysSetting().getPostSetting().getPostIntegral();
        if (postIntegral > 0 && ArticleStatusEnum.AUDIT.equals(article.getStatus())) {
            userInfoService.updateUserIntegral(article.getUserId(), UserIntegralOperTypeEnum.POST_ARTICLE, UserIntegralChangeTypeEnum.ADD.getChangeType(), postIntegral);
        }
    }

    /**
     * 修改文章， 更新文章
     *
     * @param isAdmin
     * @param article
     * @param articleAttachment
     * @param cover
     * @param attachment
     */
    @Override
    public void updateArticle(Boolean isAdmin, ForumArticle article, ForumArticleAttachment articleAttachment, MultipartFile cover, MultipartFile attachment) {
        ForumArticle dbInfo = forumArticleMapper.selectByArticleId(article.getArticleId());
        if (!isAdmin && !dbInfo.getUserId().equals(article.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        article.setLastUpdateTime(new Date());
        resetBoardInfo(isAdmin, article);
        if (cover != null) {
            FileUploadDto fileUploadDto = fileUtils.uploadFile2Local(cover, Constants.FILE_FOLDER_IMAGE, FileUploadTypeEnum.ARTICLE_COVER);
            article.setCover(fileUploadDto.getLocalPath());
        }

        if (attachment != null) {
            uploadAttachment(article, articleAttachment, attachment, true);
            article.setAttachmentType(ArticleAttachmentTypeEnum.HAVE_ATTACHMENT.getType());
        }

        ForumArticleAttachment dbAttachment = null;
        ForumArticleAttachmentQuery articleAttachmentQuery = new ForumArticleAttachmentQuery();
        articleAttachmentQuery.setArticleId(article.getArticleId());
        List<ForumArticleAttachment> articleAttachmentList = this.forumArticleAttachmentMapper.selectList(articleAttachmentQuery);
        if (!articleAttachmentList.isEmpty()) {
            dbAttachment = articleAttachmentList.get(0);
        }

        if (dbAttachment != null) {
            if (article.getAttachmentType() == Constants.ZERO) {
                new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_ATTACHMENT + dbAttachment.getFilePath()).delete();
                this.forumArticleAttachmentMapper.deleteByFileId(dbAttachment.getFileId());
            } else {
                // 更新积分
                if (!dbAttachment.getIntegral().equals(articleAttachment.getIntegral())) {
                    ForumArticleAttachment integralUpdate = new ForumArticleAttachment();
                    integralUpdate.setIntegral(articleAttachment.getIntegral());
                    this.forumArticleAttachmentMapper.updateByFileId(integralUpdate, integralUpdate.getFileId());
                }
            }
        }

        // 文章审核信息
        if (isAdmin) {
            article.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        } else {
            SysSetting4AuditDto auditDto = SysCacheUtils.getSysSetting().getAuditSetting();
            article.setStatus(auditDto.getPostAudit() ? ArticleStatusEnum.NO_AUDIT.getStatus() : ArticleStatusEnum.AUDIT.getStatus());
        }

        // 替换图片ta
        String content = article.getContent();
        if (!StringTools.isEmpty(content)) {
            String month = imageUtils.resetImageHtml(content);
            String replaceMonth = "/" + month + "/";
            content = content.replace(Constants.FILE_FOLDER_TEMP, replaceMonth);
            article.setContent(content);
            String markdownContent = article.getMarkdownContent();
            if (!StringTools.isEmpty(markdownContent)) {
                markdownContent = markdownContent.replace(Constants.FILE_FOLDER_TEMP, replaceMonth);
                article.setMarkdownContent(markdownContent);
            }
        }
        this.forumArticleMapper.updateByArticleId(article, article.getArticleId());
    }

    private void resetBoardInfo(Boolean isAdmin, ForumArticle forumArticle) {
        ForumBoard pBoard = forumBoardService.getForumBoardByBoardId(forumArticle.getpBoardId());
        if (pBoard == null || (pBoard.getPostType() == Constants.ZERO && !isAdmin)) {
            throw new BusinessException("一级板块不存在");
        }
        forumArticle.setpBoardName(pBoard.getBoardName());

        if (forumArticle.getBoardId() != null && forumArticle.getBoardId() != 0) {
            ForumBoard board = forumBoardService.getForumBoardByBoardId(forumArticle.getBoardId());
            if (board == null || (board.getPostType() == Constants.ZERO && !isAdmin)) {
                throw new BusinessException("二级板块不存在");
            }
            forumArticle.setBoardName((board.getBoardName()));
        } else {
            forumArticle.setBoardId(0);
            forumArticle.setBoardName("");
        }

    }

    public void uploadAttachment(ForumArticle article, ForumArticleAttachment articleAttachment, MultipartFile file, Boolean isUpdate) {
        Integer allowSizeMb = SysCacheUtils.getSysSetting().getPostSetting().getAttachmentSize();
        long allowSize = allowSizeMb * Constants.FILE_SIZE_1M;
        if (file.getSize() > allowSize) {
            throw new BusinessException("附件最大只能上传" + allowSize + "MB");
        }
        // 修改文章时附件处理
        ForumArticleAttachment dbInfo = null;
        if (isUpdate) {
            ForumArticleAttachmentQuery articleAttachmentQuery = new ForumArticleAttachmentQuery();
            articleAttachmentQuery.setArticleId(article.getArticleId());
            List<ForumArticleAttachment> articleAttachmentList = this.forumArticleAttachmentMapper.selectList(articleAttachmentQuery);
            if (!articleAttachmentList.isEmpty()) {
                dbInfo = articleAttachmentList.get(0);
                new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_ATTACHMENT + dbInfo.getFilePath()).delete();
            }
        }

        FileUploadDto fileUploadDto = fileUtils.uploadFile2Local(file, Constants.FILE_FOLDER_ATTACHMENT, FileUploadTypeEnum.ARTICLE_ATTACHMENT);

        if (dbInfo == null) {
            articleAttachment.setFileId(StringTools.getRandomNumber(Constants.LENGTH_15));
            articleAttachment.setArticleId(article.getArticleId());
            articleAttachment.setFileName(fileUploadDto.getOriginalFileName());
            articleAttachment.setFilePath(fileUploadDto.getLocalPath());
            articleAttachment.setFileSize(file.getSize());
            articleAttachment.setDownloadCount(Constants.ZERO);
            articleAttachment.setFileType(AttachmentFileTypeEnum.ZIP.getType());
            articleAttachment.setUserId(article.getUserId());
            forumArticleAttachmentMapper.insert(articleAttachment);
        } else {
            ForumArticleAttachment updateInfo = new ForumArticleAttachment();
            updateInfo.setFileName(fileUploadDto.getOriginalFileName());
            updateInfo.setFileSize(file.getSize());
            updateInfo.setFilePath(fileUploadDto.getLocalPath());
            forumArticleAttachmentMapper.updateByFileId(updateInfo, dbInfo.getFileId());
        }
    }

    @Override
    public void delArticle(String articleIds) {
        String[] articleIdArray = articleIds.split(",");
        for (String articleId : articleIdArray) {
            // 这里使用this.forumArticleService操作不生效
            // 必须重新在上面定义一个ForumArticleService forumArticleService
            forumArticleService.delArticleSingle(articleId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delArticleSingle(String articleId) {
        ForumArticle article = getForumArticleByArticleId(articleId);
        if (article == null || ArticleStatusEnum.DEL.getStatus().equals(article.getStatus())) {
            return;
        }
        ForumArticle updateInfo = new ForumArticle();
        updateInfo.setStatus(ArticleStatusEnum.DEL.getStatus());
        forumArticleMapper.updateByArticleId(updateInfo, articleId);

        Integer integral = SysCacheUtils.getSysSetting().getPostSetting().getPostIntegral();
        if (integral > 0 && ArticleStatusEnum.AUDIT.getStatus().equals(article.getStatus())) {
            userInfoService.updateUserIntegral(article.getUserId(), UserIntegralOperTypeEnum.DEL_ARTICLE, UserIntegralChangeTypeEnum.REDUCE.getChangeType(), integral);
        }
        UserMessage userMessage = new UserMessage();
        userMessage.setReceivedUserId(article.getUserId());
        userMessage.setMessageType(MessageTypeEnum.SYS.getType());
        userMessage.setCreateTime(new Date());
        userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
        userMessage.setMessageContent("文章【" + article.getTitle() + "】被管理员删除");
        userMessageService.add(userMessage);
    }

    @Override
    public void updateBoard(String articleId, Integer pBoardId, Integer boardId) {
        ForumArticle forumArticle = new ForumArticle();
        forumArticle.setpBoardId(pBoardId);
        forumArticle.setBoardId(boardId);
        resetBoardInfo(true, forumArticle);
        forumArticleMapper.updateByArticleId(forumArticle, articleId);
    }

    @Override
    public void auditArticle(String articleIds) {
        String[] articleIdArray = articleIds.split(",");
        for (String articleId : articleIdArray) {
            forumArticleService.auditArticleSingle(articleId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditArticleSingle(String articleId) {
        ForumArticle article = getForumArticleByArticleId(articleId);
        if (article == null || !ArticleStatusEnum.NO_AUDIT.getStatus().equals(article.getStatus())) {
            return;
        }

        ForumArticle updateInfo = new ForumArticle();
        updateInfo.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        forumArticleMapper.updateByArticleId(updateInfo, articleId);

        Integer integral = SysCacheUtils.getSysSetting().getPostSetting().getPostIntegral();
        if (integral > 0 && ArticleStatusEnum.AUDIT.getStatus().equals(article.getStatus())) {
            userInfoService.updateUserIntegral(article.getUserId(), UserIntegralOperTypeEnum.POST_ARTICLE, UserIntegralChangeTypeEnum.ADD.getChangeType(), integral);
        }
    }
}