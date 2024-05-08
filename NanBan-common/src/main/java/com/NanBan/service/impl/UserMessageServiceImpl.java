package com.NanBan.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.NanBan.entity.dto.UserMessageCountDto;
import com.NanBan.entity.enums.MessageStatusEnum;
import com.NanBan.entity.enums.MessageTypeEnum;
import org.springframework.stereotype.Service;

import com.NanBan.entity.enums.PageSize;
import com.NanBan.entity.query.UserMessageQuery;
import com.NanBan.entity.po.UserMessage;
import com.NanBan.entity.vo.PaginationResultVO;
import com.NanBan.entity.query.SimplePage;
import com.NanBan.mappers.UserMessageMapper;
import com.NanBan.service.UserMessageService;
import com.NanBan.utils.StringTools;


/**
 * 用户消息 业务接口实现
 */
@Service("userMessageService")
public class UserMessageServiceImpl implements UserMessageService {

	@Resource
	private UserMessageMapper<UserMessage, UserMessageQuery> userMessageMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserMessage> findListByParam(UserMessageQuery param) {
		return this.userMessageMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserMessageQuery param) {
		return this.userMessageMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserMessage> findListByPage(UserMessageQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserMessage> list = this.findListByParam(param);
		PaginationResultVO<UserMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserMessage bean) {
		return this.userMessageMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userMessageMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userMessageMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserMessage bean, UserMessageQuery param) {
		StringTools.checkParam(param);
		return this.userMessageMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserMessageQuery param) {
		StringTools.checkParam(param);
		return this.userMessageMapper.deleteByParam(param);
	}

	/**
	 * 根据MessageId获取对象
	 */
	@Override
	public UserMessage getUserMessageByMessageId(Integer messageId) {
		return this.userMessageMapper.selectByMessageId(messageId);
	}

	/**
	 * 根据MessageId修改
	 */
	@Override
	public Integer updateUserMessageByMessageId(UserMessage bean, Integer messageId) {
		return this.userMessageMapper.updateByMessageId(bean, messageId);
	}

	/**
	 * 根据MessageId删除
	 */
	@Override
	public Integer deleteUserMessageByMessageId(Integer messageId) {
		return this.userMessageMapper.deleteByMessageId(messageId);
	}

	/**
	 * 根据ArticleIdAndCommentIdAndSendUserIdAndMessageType获取对象
	 */
	@Override
	public UserMessage getUserMessageByArticleIdAndCommentIdAndSendUserIdAndMessageType(String articleId, Integer commentId, String sendUserId, Integer messageType) {
		return this.userMessageMapper.selectByArticleIdAndCommentIdAndSendUserIdAndMessageType(articleId, commentId, sendUserId, messageType);
	}

	/**
	 * 根据ArticleIdAndCommentIdAndSendUserIdAndMessageType修改
	 */
	@Override
	public Integer updateUserMessageByArticleIdAndCommentIdAndSendUserIdAndMessageType(UserMessage bean, String articleId, Integer commentId, String sendUserId, Integer messageType) {
		return this.userMessageMapper.updateByArticleIdAndCommentIdAndSendUserIdAndMessageType(bean, articleId, commentId, sendUserId, messageType);
	}

	/**
	 * 根据ArticleIdAndCommentIdAndSendUserIdAndMessageType删除
	 */
	@Override
	public Integer deleteUserMessageByArticleIdAndCommentIdAndSendUserIdAndMessageType(String articleId, Integer commentId, String sendUserId, Integer messageType) {
		return this.userMessageMapper.deleteByArticleIdAndCommentIdAndSendUserIdAndMessageType(articleId, commentId, sendUserId, messageType);
	}

	@Override
	public UserMessageCountDto getUserMessageCount(String userId) {
		List<Map> mapList = this.userMessageMapper.selectUserMessageCount(userId);
		UserMessageCountDto messageCountDto = new UserMessageCountDto();
		Long totalCount = 0L;
		for (Map item : mapList){
			Integer type = (Integer) item.get("messageType");
			Long count = (Long) item.get("count");
			totalCount = totalCount + count;
			MessageTypeEnum messageType = MessageTypeEnum.getByType(type);
			switch (messageType){
				case SYS:
					messageCountDto.setSys(count);
					break;
				case COMMENT:
					messageCountDto.setReply(count);
					break;
				case ARTICLE_LIKE:
					messageCountDto.setLikePost(count);
					break;
				case COMMENT_LIKE:
					messageCountDto.setLikeComment(count);
					break;
				case DOWNLOAD_ATTACHMENT:
					messageCountDto.setDownloadAttachment(count);
					break;
			}
		}
		messageCountDto.setTotal(totalCount);
		return messageCountDto;
	}

	@Override
	public void readMessageByType(String receivedUserId, Integer messageType) {
		this.userMessageMapper.updateMessageStatusBatch(receivedUserId, messageType, MessageStatusEnum.READ.getStatus());
	}
}