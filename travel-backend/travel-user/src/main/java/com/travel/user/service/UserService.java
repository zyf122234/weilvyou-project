package com.travel.user.service;

import com.travel.api.user.LoginDTO;
import com.travel.api.user.UserBalanceDeductDTO;
import com.travel.api.user.LoginVO;
import com.travel.api.user.MerchantApplicationVO;
import com.travel.api.user.RegisterDTO;
import com.travel.api.user.UserRechargeDTO;
import com.travel.api.user.UserUpdateDTO;
import com.travel.api.user.UserVO;
import com.travel.common.result.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    void register(RegisterDTO dto);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto);

    void logout(String token);

    MerchantApplicationVO applyForMerchant(Long userId);

    MerchantApplicationVO getLatestMerchantApplication(Long userId, String currentToken);

    /**
     * 根据ID获取用户信息
     */
    UserVO getUserById(Long id);

    /**
     * 根据用户名获取用户信息
     */
    UserVO getUserByUsername(String username);

    /**
     * 查询商家摘要信息
     */
    List<UserVO> listMerchants(String keyword, List<Long> ids);

    UserVO updateUserInfo(Long userId, UserUpdateDTO dto);

    UserVO rechargeBalance(Long userId, UserRechargeDTO dto);

    UserVO deductBalance(Long userId, UserBalanceDeductDTO dto);

    /**
     * 上传用户头像
     */
    String uploadAvatar(Long userId, MultipartFile file);

    // ==================== 管理接口 ====================

    /**
     * 查询用户列表
     */
    PageResult<UserVO> listUsers(Long operatorId, Long current, Long size, String keyword, Integer status, String role);

    /**
     * 更新用户状态
     */
    UserVO updateUserStatus(Long operatorId, Long userId, Integer status);

    /**
     * 查询商户申请列表
     */
    PageResult<MerchantApplicationVO> listMerchantApplications(Long operatorId, Long current, Long size, Integer status);

    /**
     * 审核商户申请
     */
    MerchantApplicationVO reviewMerchantApplication(Long operatorId, Long applicationId, Integer status, String reason);

    MerchantApplicationVO getMerchantApplicationById(Long operatorId, Long applicationId);
}
