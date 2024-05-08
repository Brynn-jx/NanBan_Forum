package com.NanBan.controller.base;
import com.NanBan.entity.constants.Constants;
import com.NanBan.entity.dto.SessionWebUserDto;
import com.NanBan.entity.enums.ResponseCodeEnum;
import com.NanBan.entity.vo.PaginationResultVO;
import com.NanBan.entity.vo.ResponseVO;
import com.NanBan.exception.BusinessException;
import com.NanBan.utils.CopyTools;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


public class ABaseController {

    protected static final String STATUC_SUCCESS = "success";

    protected static final String STATUC_ERROR = "error";

    protected <T> ResponseVO getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUC_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    protected <T> ResponseVO getBusinessErrorResponseVO(BusinessException e, T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUC_ERROR);
        if (e.getCode() == null) {
            vo.setCode(ResponseCodeEnum.CODE_600.getCode());
        } else {
            vo.setCode(e.getCode());
        }
        vo.setInfo(e.getMessage());
        vo.setData(t);
        return vo;
    }

    protected <T> ResponseVO getServerErrorResponseVO(T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUC_ERROR);
        vo.setCode(ResponseCodeEnum.CODE_500.getCode());
        vo.setInfo(ResponseCodeEnum.CODE_500.getMsg());
        vo.setData(t);
        return vo;
    }

    protected String getIpAddr(HttpServletRequest request){
        String ip = request.getHeader("x-forwarded-for");
        if(ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)){
            // 多次反向代理后会有多个ip值，第一个才是真实的ip
            if(ip.indexOf(",") != -1){
                ip = ip.split(",")[0];
            }
        }

        if(ip == null || ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)){
            ip = request.getHeader("Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || !"unknown".equalsIgnoreCase(ip)){
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || !"unknown".equalsIgnoreCase(ip)){
            ip = request.getHeader("HTTP_Client_IP");
        }
        if(ip == null || ip.length() == 0 || !"unknown".equalsIgnoreCase(ip)){
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if(ip == null || ip.length() == 0 || !"unknown".equalsIgnoreCase(ip)){
            ip = request.getHeader("X-Real-IP");
        }
        if(ip == null || ip.length() == 0 || !"unknown".equalsIgnoreCase(ip)){
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    protected SessionWebUserDto getUserInfoFromSession(HttpSession session){
        SessionWebUserDto sessionWebUserDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        return  sessionWebUserDto;
    }

    protected <S, T>PaginationResultVO<T> convert2PaginationVO(PaginationResultVO<S> result, Class<T> clazz){
        PaginationResultVO<T> resultVO = new PaginationResultVO<>();
        resultVO.setList(CopyTools.copyList(result.getList(), clazz));
        resultVO.setPageNo(result.getPageNo());
        resultVO.setPageSize(result.getPageSize());
        resultVO.setPageTotal(result.getPageTotal());
        resultVO.setTotalCount(result.getTotalCount());
        return resultVO;
    }
}
