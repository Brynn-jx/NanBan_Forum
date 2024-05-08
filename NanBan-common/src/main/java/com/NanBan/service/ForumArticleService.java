package com.NanBan.service;

import com.NanBan.entity.po.ForumArticle;
import com.NanBan.entity.po.ForumArticleAttachment;
import com.NanBan.entity.query.ForumArticleQuery;
import com.NanBan.entity.vo.PaginationResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.net.Inet4Address;
import java.util.List;


/**
 * 文章信息 业务接口
 */
public interface ForumArticleService {

    /**
     * 根据条件查询列表
     */
    List<ForumArticle> findListByParam(ForumArticleQuery param);

    /**
     * 根据条件查询列表
     */
    Integer findCountByParam(ForumArticleQuery param);

    /**
     * 分页查询
     */
    PaginationResultVO<ForumArticle> findListByPage(ForumArticleQuery param);

    /**
     * 新增
     */
    Integer add(ForumArticle bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<ForumArticle> listBean);

    /**
     * 批量新增/修改
     */
    Integer addOrUpdateBatch(List<ForumArticle> listBean);

    /**
     * 多条件更新
     */
    Integer updateByParam(ForumArticle bean, ForumArticleQuery param);

    /**
     * 多条件删除
     */
    Integer deleteByParam(ForumArticleQuery param);

    /**
     * 根据ArticleId查询对象
     */
    ForumArticle getForumArticleByArticleId(String articleId);


    /**
     * 根据ArticleId修改
     */
    Integer updateForumArticleByArticleId(ForumArticle bean, String articleId);


    /**
     * 根据ArticleId删除
     */
    Integer deleteForumArticleByArticleId(String articleId);

    ForumArticle readArticle(String articleId);

    void postArticle(Boolean isAdmin, ForumArticle article, ForumArticleAttachment articleAttachment, MultipartFile cover, MultipartFile attachment);

    void updateArticle(Boolean isAdmin, ForumArticle article, ForumArticleAttachment articleAttachment, MultipartFile cover, MultipartFile attachment);

    void delArticle(String articleIds);

    void delArticleSingle(String articleId);

    void updateBoard(String articleId, Integer pBoardId, Integer boardId);

    void auditArticle(String articleIds);

    void auditArticleSingle(String articleId);

}