package com.NanBan.controller;

import com.NanBan.annotation.GlobalInterceptor;
import com.NanBan.annotation.VerifyParam;
import com.NanBan.controller.base.ABaseController;
import com.NanBan.entity.config.AdminConfig;
import com.NanBan.entity.constants.Constants;
import com.NanBan.entity.po.ForumArticle;
import com.NanBan.entity.po.ForumArticleAttachment;
import com.NanBan.entity.query.ForumArticleAttachmentQuery;
import com.NanBan.entity.query.ForumArticleQuery;
import com.NanBan.entity.query.ForumCommentQuery;
import com.NanBan.entity.vo.ResponseVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.service.ForumArticleAttachmentService;
import com.NanBan.service.ForumArticleService;
import com.NanBan.service.ForumCommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/forum")
public class ForumArticleController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(ForumArticleController.class);
    @Resource
    private ForumArticleService forumArticleService;

    @Resource
    private ForumArticleAttachmentService forumArticleAttachmentService;

    @Resource
    private AdminConfig adminConfig;

    @Resource
    private ForumCommentService forumCommentService;

    @RequestMapping("/loadArticle")
    public ResponseVO loadArticle(ForumArticleQuery articleQuery) {
        articleQuery.setOrderBy("post_time desc");
        return getSuccessResponseVO(forumArticleService.findListByPage(articleQuery));
    }

    @RequestMapping("/delArticle")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO delArticle(@VerifyParam(required = true) String articleIds) {
        forumArticleService.delArticle(articleIds);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/updateBoard")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO updateBoard(@VerifyParam(required = true) String articleId,
                                  @VerifyParam(required = true) Integer pBoardId,
                                  @VerifyParam(required = true) Integer boardId) {
        boardId = boardId == null ? 0 : boardId;
        forumArticleService.updateBoard(articleId, pBoardId, boardId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/getAttachment")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getAttachment(@VerifyParam(required = true) String articleId) {
        ForumArticleAttachmentQuery articleAttachmentQuery = new ForumArticleAttachmentQuery();
        articleAttachmentQuery.setArticleId(articleId);
        List<ForumArticleAttachment> attachmentList = forumArticleAttachmentService.findListByParam(articleAttachmentQuery);
        if (attachmentList.isEmpty()) {
            throw new BusinessException("附件不存在");
        }
        return getSuccessResponseVO(attachmentList.get(0));
    }

    @RequestMapping("/attachmentDownload")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public void attachmentDownload(HttpServletRequest request, HttpServletResponse response,
                                   @VerifyParam(required = true) String fileId) {
        ForumArticleAttachment attachment = forumArticleAttachmentService.getForumArticleAttachmentByFileId(fileId);
        InputStream in = null;
        OutputStream out = null;
        String downloadFileName = attachment.getFileName();
        String filePath = adminConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_ATTACHMENT + attachment.getFilePath();
        File file = new File(filePath);
        try {
            in = new FileInputStream(file);
            out = response.getOutputStream();
            response.setContentType("application/x-msdownload; charset=UTF-8");
            // 解决中文文件乱码问题
            if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {
                // IE浏览器
                downloadFileName = URLEncoder.encode(downloadFileName, "UTF-8");
            } else {
                downloadFileName = new String(downloadFileName.getBytes("UTF-8"), "ISO8859-1");
            }
            response.setHeader("Content-Disposition", "attachment;filename=\"" + downloadFileName + "\"");
            byte[] byteData = new byte[1024];
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            logger.error("下载异常", e);
            throw new BusinessException("下载失败");
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                logger.error("IO异常", e);
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                logger.error("IO异常", e);
            }
        }
    }

    @RequestMapping("/topArticle")
    public ResponseVO topArticle(@VerifyParam(required = true) String articleId, Integer topType) {
        ForumArticle forumArticle = new ForumArticle();
        forumArticle.setTopType(topType);
        forumArticleService.updateForumArticleByArticleId(forumArticle, articleId);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/auditArticle")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO auditArticle(@VerifyParam(required = true) String articleIds) {
        forumArticleService.auditArticle(articleIds);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/loadComment")
    public ResponseVO loadComment(ForumCommentQuery forumCommentQuery) {
        forumCommentQuery.setLoadChildren(true);
        forumCommentQuery.setOrderBy("post_time desc");
        return getSuccessResponseVO(forumCommentService.findListByPage(forumCommentQuery));
    }

    @RequestMapping("/loadComment4Article")
    public ResponseVO loadComment4Article(ForumCommentQuery forumCommentQuery) {
        forumCommentQuery.setLoadChildren(true);
        forumCommentQuery.setOrderBy("post_time desc");
        forumCommentQuery.setpCommentId(0);
        return getSuccessResponseVO(forumCommentService.findListByParam(forumCommentQuery));
    }

    @RequestMapping("/delComment")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO delComment(@VerifyParam(required = true) String commentIds) {
        forumCommentService.delComment(commentIds);
        return getSuccessResponseVO(null);
    }


    @RequestMapping("/auditComment")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO auditComment(@VerifyParam(required = true) String commentIds) {
        forumCommentService.auditComment(commentIds);
        return getSuccessResponseVO(null);
    }
}
