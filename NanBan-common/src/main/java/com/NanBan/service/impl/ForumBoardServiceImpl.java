package com.NanBan.service.impl;

import com.NanBan.entity.enums.PageSize;
import com.NanBan.entity.po.ForumArticle;
import com.NanBan.entity.po.ForumBoard;
import com.NanBan.entity.query.ForumArticleQuery;
import com.NanBan.entity.query.ForumBoardQuery;
import com.NanBan.entity.query.SimplePage;
import com.NanBan.entity.vo.PaginationResultVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.mappers.ForumArticleMapper;
import com.NanBan.mappers.ForumBoardMapper;
import com.NanBan.service.ForumBoardService;
import com.NanBan.utils.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


/**
 * 文章板块信息 业务接口实现
 */
@Service("forumBoardService")
public class ForumBoardServiceImpl implements ForumBoardService {

    @Resource
    private ForumBoardMapper<ForumBoard, ForumBoardQuery> forumBoardMapper;

    @Resource
    private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<ForumBoard> findListByParam(ForumBoardQuery param) {
        return this.forumBoardMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(ForumBoardQuery param) {
        return this.forumBoardMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<ForumBoard> findListByPage(ForumBoardQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<ForumBoard> list = this.findListByParam(param);
        PaginationResultVO<ForumBoard> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(ForumBoard bean) {
        return this.forumBoardMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<ForumBoard> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumBoardMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<ForumBoard> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumBoardMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(ForumBoard bean, ForumBoardQuery param) {
        StringTools.checkParam(param);
        return this.forumBoardMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(ForumBoardQuery param) {
        StringTools.checkParam(param);
        return this.forumBoardMapper.deleteByParam(param);
    }

    /**
     * 根据BoardId获取对象
     */
    @Override
    public ForumBoard getForumBoardByBoardId(Integer boardId) {
        return this.forumBoardMapper.selectByBoardId(boardId);
    }

    /**
     * 根据BoardId修改
     */
    @Override
    public Integer updateForumBoardByBoardId(ForumBoard bean, Integer boardId) {
        return this.forumBoardMapper.updateByBoardId(bean, boardId);
    }

    /**
     * 根据BoardId删除
     */
    @Override
    public Integer deleteForumBoardByBoardId(Integer boardId) {
        return this.forumBoardMapper.deleteByBoardId(boardId);
    }

    @Override
    public List<ForumBoard> getBoardTree(Integer postType) {
        ForumBoardQuery boardQuery = new ForumBoardQuery();
        boardQuery.setOrderBy("sort asc");
        boardQuery.setPostType(postType);
        List<ForumBoard> forumBoardList = this.forumBoardMapper.selectList(boardQuery);

        return convertLine2Tree(forumBoardList, 0);
    }

    private List<ForumBoard> convertLine2Tree(List<ForumBoard> dataList, Integer pid) {
        List<ForumBoard> children = new ArrayList<>();
        for (ForumBoard forumBoard : dataList) {
            if (forumBoard.getpBoardId().equals(pid)) {
                forumBoard.setChildren(convertLine2Tree(dataList, forumBoard.getBoardId()));
                children.add(forumBoard);
            }
        }
        return children;
    }

    /**
     * boardId不存在的话就是新增板块
     * 存在的话就是修改板块
     *
     * @param forumBoard
     */
    @Override
    public void saveForumBoard(ForumBoard forumBoard) {
        if (forumBoard.getBoardId() == null) {
            ForumBoardQuery query = new ForumBoardQuery();
            query.setpBoardId(forumBoard.getpBoardId());
            Integer count = this.forumBoardMapper.selectCount(query);
            // 排序方法就是每新增一个板块就增加1, 按照大小排序
            forumBoard.setSort(count + 1);
            this.forumBoardMapper.insert(forumBoard);
        } else {
            ForumBoard dbInfo = this.forumBoardMapper.selectByBoardId(forumBoard.getBoardId());
            if (dbInfo == null) {
                throw new BusinessException("板块信息不存在");
            }
            this.forumBoardMapper.updateByBoardId(forumBoard, forumBoard.getBoardId());
            if (!dbInfo.getBoardName().equals(forumBoard.getBoardName())) {
                forumArticleMapper.updateBoardNameBatch(dbInfo.getpBoardId() == 0 ? 0 : 1, forumBoard.getBoardName(), forumBoard.getBoardId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeSort(String boardIds) {
        String[] boardIdArray = boardIds.split(",");
        Integer index = 1;
        for (String boardIdStr : boardIdArray) {
            Integer boardId = Integer.parseInt(boardIdStr);
            ForumBoard board = new ForumBoard();
            board.setSort(index);
            forumBoardMapper.updateByBoardId(board, boardId);
            index++;
        }
    }
}