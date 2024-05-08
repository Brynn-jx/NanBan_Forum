package com.NanBan.service;

import com.NanBan.entity.dto.SessionWebUserDto;
import com.NanBan.entity.enums.UserIntegralOperTypeEnum;
import com.NanBan.entity.po.UserInfo;
import com.NanBan.entity.query.UserInfoQuery;
import com.NanBan.entity.vo.PaginationResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


/**
 * 用户信息 业务接口
 */
public interface UserInfoService {

    /**
     * 根据条件查询列表
     */
    List<UserInfo> findListByParam(UserInfoQuery param);

    /**
     * 根据条件查询列表
     */
    Integer findCountByParam(UserInfoQuery param);

    /**
     * 分页查询
     */
    PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param);

    /**
     * 新增
     */
    Integer add(UserInfo bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<UserInfo> listBean);

    /**
     * 批量新增/修改
     */
    Integer addOrUpdateBatch(List<UserInfo> listBean);

    /**
     * 多条件更新
     */
    Integer updateByParam(UserInfo bean, UserInfoQuery param);

    /**
     * 多条件删除
     */
    Integer deleteByParam(UserInfoQuery param);

    /**
     * 根据UserId查询对象
     */
    UserInfo getUserInfoByUserId(String userId);


    /**
     * 根据UserId修改
     */
    Integer updateUserInfoByUserId(UserInfo bean, String userId);


    /**
     * 根据UserId删除
     */
    Integer deleteUserInfoByUserId(String userId);


    /**
     * 根据Email查询对象
     */
    UserInfo getUserInfoByEmail(String email);


    /**
     * 根据Email修改
     */
    Integer updateUserInfoByEmail(UserInfo bean, String email);


    /**
     * 根据Email删除
     */
    Integer deleteUserInfoByEmail(String email);


    /**
     * 根据NickName查询对象
     */
    UserInfo getUserInfoByNickName(String nickName);


    /**
     * 根据NickName修改
     */
    Integer updateUserInfoByNickName(UserInfo bean, String nickName);


    /**
     * 根据NickName删除
     */
    Integer deleteUserInfoByNickName(String nickName);

    /**
     * 登陆函数
     */
    void register(String email, String emailCode, String nickName, String password);

    void updateUserIntegral(String userId, UserIntegralOperTypeEnum opTypeEnum, Integer changeType, Integer integral);

    SessionWebUserDto login(String email, String password, String ip);

    void resetPwd(String email, String password, String emailCode);

    /**
     * 用户中心更改用户信息
     *
     * @param userInfo
     * @param avatar
     */
    void updateUserInfo(UserInfo userInfo, MultipartFile avatar);

    void updateUserStatus(Integer status, String userId);

    void sendMessage(String userId, String message, Integer integral);
}