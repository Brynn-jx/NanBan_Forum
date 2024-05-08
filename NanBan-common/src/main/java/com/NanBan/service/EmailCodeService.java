package com.NanBan.service;

import java.util.List;

import com.NanBan.entity.query.EmailCodeQuery;
import com.NanBan.entity.po.EmailCode;
import com.NanBan.entity.vo.PaginationResultVO;


/**
 * 邮箱验证码 业务接口
 */
public interface EmailCodeService {

	/**
	 * 根据条件查询列表
	 */
	List<EmailCode> findListByParam(EmailCodeQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(EmailCodeQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<EmailCode> findListByPage(EmailCodeQuery param);

	/**
	 * 新增
	 */
	Integer add(EmailCode bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<EmailCode> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<EmailCode> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(EmailCode bean,EmailCodeQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(EmailCodeQuery param);

	/**
	 * 根据EmailAndCode查询对象
	 */
	EmailCode getEmailCodeByEmailAndCode(String email,String code);


	/**
	 * 根据EmailAndCode修改
	 */
	Integer updateEmailCodeByEmailAndCode(EmailCode bean,String email,String code);


	/**
	 * 根据EmailAndCode删除
	 */
	Integer deleteEmailCodeByEmailAndCode(String email,String code);

	/**
	 * 发送邮箱验证码
	 */
	void sendEmailCode(String email,Integer type);

	void checkCode(String email,String emailCode);

}