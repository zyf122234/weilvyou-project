package com.travel.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.api.user.LoginDTO;
import com.travel.api.user.LoginVO;
import com.travel.api.user.MerchantApplicationVO;
import com.travel.api.user.RegisterDTO;
import com.travel.api.user.UserBalanceDeductDTO;
import com.travel.api.user.UserRechargeDTO;
import com.travel.api.user.UserUpdateDTO;
import com.travel.api.user.UserVO;
import com.travel.common.constant.CommonConstant;
import com.travel.common.exception.BusinessException;
import com.travel.common.result.PageResult;
import com.travel.common.security.TokenBlacklistService;
import com.travel.common.utils.JwtUtils;
import com.travel.common.utils.PageUtils;
import com.travel.user.config.MerchantApplicationRabbitConfig;
import com.travel.user.config.UserCacheRabbitConfig;
import com.travel.user.entity.MerchantApplication;
import com.travel.user.entity.SysRole;
import com.travel.user.entity.SysUser;
import com.travel.user.entity.SysUserRole;
import com.travel.user.mapper.LoginUserRecord;
import com.travel.user.mapper.MerchantApplicationMapper;
import com.travel.user.mapper.SysRoleMapper;
import com.travel.user.mapper.SysUserMapper;
import com.travel.user.mapper.SysUserRoleMapper;
import com.travel.user.mq.UserCacheWarmupMessage;
import com.travel.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final MerchantApplicationMapper applicationMapper;
    private final RabbitTemplate rabbitTemplate;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${travel.upload.avatar-dir}")
    private String avatarDir;

    private static final String USER_INFO_CACHE = "user:info";
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");


    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setBalance(BigDecimal.ZERO.setScale(2));
        user.setStatus(1);
        try {
            userMapper.insert(user);
            //捕获唯一索引产生的异常
        } catch (DuplicateKeyException e) {
            throw new BusinessException("用户名已存在");
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(2L);
        userRoleMapper.insert(userRole);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        LoginUserRecord user = userMapper.selectLoginUserByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(403, "该账号已被禁用，请联系超级管理员");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        List<String> roles = splitRoles(user.getRoles());
        String rolesStr = String.join(",", roles);
        String loginSessionId = JwtUtils.newLoginSessionId();
        String token = JwtUtils.generateToken(user.getId(), user.getUsername(), rolesStr, loginSessionId);

        sendUserCacheWarmup(user.getId());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setLoginSessionId(loginSessionId);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRoles(roles);
        return vo;
    }

    private List<String> splitRoles(String roles) {
        if (!StringUtils.hasText(roles)) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .filter(StringUtils::hasText)
                .toList();
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        //计算token剩余时间
        long remainingMillis = JwtUtils.getRemainingMillis(token);
        tokenBlacklistService.blacklist(token, Duration.ofMillis(remainingMillis));
    }


    //申请成为商户
    @Override
    @Transactional
    public MerchantApplicationVO applyForMerchant(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(403, "用户账号异常");
        }


        // 已经拥有商户角色时，直接返回当前申请状态，避免重复发送 MQ。
        if (userMapper.selectRoleCodesByUserId(userId).contains(CommonConstant.ROLE_MERCHANT)) {
            return toMerchantApplicationVO(latestMerchantApplication(userId), user);
        }

        // 同一用户只允许存在一条待审核申请。
        MerchantApplication pending = applicationMapper.selectOne(new LambdaQueryWrapper<MerchantApplication>()
                .eq(MerchantApplication::getUserId, userId)
                .eq(MerchantApplication::getStatus, 0)
                .last("LIMIT 1"));
        if (pending != null) {
            return toMerchantApplicationVO(pending, user);
        }

        // 申请记录属于用户申请动作，先在 user 服务直接落库。
        MerchantApplication application = new MerchantApplication();
        application.setUserId(user.getId());
        application.setStatus(0);
        applicationMapper.insert(application);

        // 事务提交后再通知 admin，避免 admin 收到消息时申请记录还没入库。
        sendMerchantApplicationAfterCommit(application.getId());
        return toMerchantApplicationVO(application, user);
    }



    @Override
    public MerchantApplicationVO getLatestMerchantApplication(Long userId, String currentToken) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        MerchantApplication application = latestMerchantApplication(userId);
        if (application == null && userMapper.selectRoleCodesByUserId(userId).contains(CommonConstant.ROLE_MERCHANT)) {
            MerchantApplication approved = new MerchantApplication();
            approved.setUserId(userId);
            approved.setStatus(1);
            return toMerchantApplicationVO(approved, user, currentToken);
        }
        return toMerchantApplicationVO(application, user, currentToken);
    }

    @Override
    @Cacheable(cacheNames = USER_INFO_CACHE, key = "#p0")
    public UserVO getUserById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToVO(user);
    }

    @Override
    public UserVO getUserByUsername(String username) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToVO(user);
    }



    //商家列表
    @Override
    public List<UserVO> listMerchants(String keyword, List<Long> ids) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .in(ids != null && !ids.isEmpty(), SysUser::getId, ids)
                .and(StringUtils.hasText(keyword), item -> item
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getNickname, keyword))
                .apply("EXISTS (SELECT 1 FROM sys_user_role ur INNER JOIN sys_role r ON r.id = ur.role_id WHERE ur.user_id = sys_user.id AND r.role_code = {0})",
                        CommonConstant.ROLE_MERCHANT)
                .orderByDesc(SysUser::getCreateTime);
        return userMapper.selectList(wrapper).stream()
                .map(this::convertToMerchantSummaryVO)
                .toList();
    }

    //更新用户信息
    @Override
    @Transactional
    public UserVO updateUserInfo(Long userId, UserUpdateDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        String oldUsername = user.getUsername();

        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId);
        boolean changed = false;

        if (StringUtils.hasText(dto.getUsername())) {
            SysUser existUser = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, dto.getUsername())
                    .ne(SysUser::getId, userId));
            if (existUser != null) {
                throw new BusinessException("用户名已存在");
            }
            updateWrapper.set(SysUser::getUsername, dto.getUsername());
            changed = true;
        }

        if (dto.getNickname() != null) {
            updateWrapper.set(SysUser::getNickname, StringUtils.hasText(dto.getNickname()) ? dto.getNickname().trim() : null);
            changed = true;
        }

        if (dto.getEmail() != null) {
            updateWrapper.set(SysUser::getEmail, StringUtils.hasText(dto.getEmail()) ? dto.getEmail() : null);
            changed = true;
        }

        if (changed) {
            userMapper.update(null, updateWrapper);
        }
        UserVO userVO = convertToVO(userMapper.selectById(userId));
        evictUserCacheAfterCommit(userVO.getId());
        return userVO;
    }

    //充值金额
    @Override
    @Transactional
    public UserVO rechargeBalance(Long userId, UserRechargeDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (dto == null || dto.getAmount() == null) {
            throw new BusinessException("充值金额不能为空");
        }
        BigDecimal amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于 0");
        }

        BigDecimal oldBalance = user.getBalance() == null
                ? BigDecimal.ZERO.setScale(2)
                : user.getBalance();
        BigDecimal newBalance = oldBalance.add(amount);
        user.setBalance(newBalance);
        userMapper.updateById(user);

        UserVO userVO = convertToVO(userMapper.selectById(userId));
        evictUserCacheAfterCommit(userVO.getId());
        return userVO;
    }

    //扣减金额
    @Override
    @Transactional
    public UserVO deductBalance(Long userId, UserBalanceDeductDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (dto == null || dto.getAmount() == null) {
            throw new BusinessException("扣减金额不能为空");
        }
        BigDecimal amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("扣减金额必须大于 0");
        }

        BigDecimal oldBalance = user.getBalance() == null
                ? BigDecimal.ZERO.setScale(2)
                : user.getBalance();
        if (oldBalance.compareTo(amount) < 0) {
            throw new BusinessException(400, "余额不足，请先充值");
        }

        BigDecimal newBalance = oldBalance.subtract(amount);
        user.setBalance(newBalance);
        userMapper.updateById(user);

        UserVO userVO = convertToVO(userMapper.selectById(userId));
        evictUserCacheAfterCommit(userVO.getId());
        return userVO;
    }

    //更新用户头像
    @Override
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择头像文件");
        }
        if (!ALLOWED_AVATAR_TYPES.contains(file.getContentType())) {
            throw new BusinessException("仅支持 JPG、PNG、WebP、GIF 格式头像");
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (!StringUtils.hasText(extension)) {
            extension = switch (file.getContentType()) {
                case "image/png" -> "png";
                case "image/webp" -> "webp";
                case "image/gif" -> "gif";
                default -> "jpg";
            };
        }

        try {
            //生成绝对路径
            Path avatarPath = Paths.get(avatarDir).toAbsolutePath().normalize();
            Files.createDirectories(avatarPath);

            String filename = userId + "-" + UUID.randomUUID() + "." + extension.toLowerCase();
            Path target = avatarPath.resolve(filename).normalize();
            file.transferTo(target);

            String avatarUrl = "/api/uploads/avatars/" + filename;
            user.setAvatarUrl(avatarUrl);
            userMapper.updateById(user);
            evictUserCacheAfterCommit(user.getId());
            return avatarUrl;
        } catch (IOException e) {
            throw new BusinessException("头像上传失败");
        }
    }


    private UserVO convertToVO(SysUser user) {
        return convertToVO(user, userMapper.selectRoleCodesByUserId(user.getId()));
    }

    private UserVO convertToVO(SysUser user, List<String> roles) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setBalance(user.getBalance() == null ? BigDecimal.ZERO.setScale(2) : user.getBalance());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setRoles(roles);

        return vo;
    }

    //获取用户最新的商户申请记录
    private MerchantApplication latestMerchantApplication(Long userId) {
        return applicationMapper.selectOne(new LambdaQueryWrapper<MerchantApplication>()
                .eq(MerchantApplication::getUserId, userId)
                .orderByDesc(MerchantApplication::getCreateTime)
                .last("LIMIT 1"));
    }

    //在事务提交后发送商户申请消息到 RabbitMQ
    private void sendMerchantApplicationAfterCommit(Long applicationId) {
        //检测当前是否有活跃的事务
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            //注册事务同步回调
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    //定义事务提交后的回调逻辑
                    sendMerchantApplication(applicationId);
                }
            });
            return;
        }
        //不在事务中 → 立即发送
        sendMerchantApplication(applicationId);
    }


    private void sendMerchantApplication(Long applicationId) {
        // RabbitMQ 只传申请ID，admin 收到后按ID读取 merchant_application。
        rabbitTemplate.convertAndSend(
                MerchantApplicationRabbitConfig.EXCHANGE,
                MerchantApplicationRabbitConfig.ROUTING_KEY,
                applicationId);
    }

    private void sendUserCacheWarmup(Long userId) {
        try {
            rabbitTemplate.convertAndSend(
                    UserCacheRabbitConfig.EXCHANGE,
                    UserCacheRabbitConfig.LOGIN_ROUTING_KEY,
                    new UserCacheWarmupMessage(userId));
        } catch (Exception e) {
            log.warn("发送用户缓存预热消息失败，用户编号={}", userId, e);
        }
    }


    private MerchantApplicationVO toMerchantApplicationVO(MerchantApplication application, SysUser user) {
        return toMerchantApplicationVO(application, user, null);
    }

    private MerchantApplicationVO toMerchantApplicationVO(MerchantApplication application, SysUser user, String currentToken) {
        if (application == null) {
            return null;
        }
        String token = null;
        if (Integer.valueOf(1).equals(application.getStatus()) && user != null) {
            List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
            if (shouldIssueTokenForNewRole(currentToken, roles)) {
                String loginSessionId = StringUtils.hasText(currentToken)
                        ? JwtUtils.getLoginSessionId(currentToken)
                        : JwtUtils.newLoginSessionId();
                token = JwtUtils.generateToken(user.getId(), user.getUsername(), String.join(",", roles),
                        loginSessionId);
                blacklistCurrentToken(currentToken);
            }
        }
        return MerchantApplicationVO.of(
                application.getId(),
                application.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getNickname(),
                user == null ? null : user.getEmail(),
                application.getStatus(),
                application.getReason(),
                token,
                application.getCreateTime(),
                application.getUpdateTime());
    }


    private boolean shouldIssueTokenForNewRole(String currentToken, List<String> roles) {
        if (!roles.contains(CommonConstant.ROLE_MERCHANT)) {
            return false;
        }
        if (!StringUtils.hasText(currentToken)) {
            return true;
        }
        String currentRoles = JwtUtils.getRoles(currentToken);
        return !StringUtils.hasText(currentRoles) || !List.of(currentRoles.split(",")).contains(CommonConstant.ROLE_MERCHANT);
    }

    private void blacklistCurrentToken(String currentToken) {
        if (!StringUtils.hasText(currentToken)) {
            return;
        }
        long remainingMillis = JwtUtils.getRemainingMillis(currentToken);
        tokenBlacklistService.blacklist(currentToken, Duration.ofMillis(remainingMillis));
    }

    private UserVO convertToMerchantSummaryVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setBalance(user.getBalance() == null ? BigDecimal.ZERO.setScale(2) : user.getBalance());
        return vo;
    }

    // ==================== 管理接口实现 ====================

    @Override
    public PageResult<UserVO> listUsers(Long operatorId, Long current, Long size, String keyword, Integer status, String role) {
        String roleCode = normalizeRole(role);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .ne(SysUser::getId, operatorId)
                .like(StringUtils.hasText(keyword), SysUser::getUsername, keyword)
                .eq(status != null, SysUser::getStatus, status)
                .apply(StringUtils.hasText(roleCode),
                        "EXISTS (SELECT 1 FROM sys_user_role ur INNER JOIN sys_role r ON r.id = ur.role_id WHERE ur.user_id = sys_user.id AND r.role_code = {0})",
                        roleCode)
                .orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> page = userMapper.selectPage(new Page<>(PageUtils.safeCurrent(current), PageUtils.safeSize(size)), wrapper);
        List<UserVO> records = page.getRecords().stream().map(this::convertToVO).toList();
        return PageResult.of(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    @Transactional
    public UserVO updateUserStatus(Long operatorId, Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("账号状态只能是正常或禁用");
        }
        if (operatorId.equals(userId) && status == 0) {
            throw new BusinessException("不能禁用当前登录的管理员账号");
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        evictUserCacheAfterCommit(user.getId());
        return convertToVO(userMapper.selectById(userId));
    }

    @Override
    public PageResult<MerchantApplicationVO> listMerchantApplications(Long operatorId, Long current, Long size, Integer status) {
        LambdaQueryWrapper<MerchantApplication> wrapper = new LambdaQueryWrapper<MerchantApplication>()
                .eq(status != null, MerchantApplication::getStatus, status)
                .orderByDesc(MerchantApplication::getCreateTime);
        IPage<MerchantApplication> page = applicationMapper.selectPage(new Page<>(PageUtils.safeCurrent(current), PageUtils.safeSize(size)), wrapper);
        List<MerchantApplicationVO> records = page.getRecords().stream()
                .map(this::toApplicationVO)
                .toList();
        return PageResult.of(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    @Transactional
    public MerchantApplicationVO reviewMerchantApplication(Long operatorId, Long applicationId, Integer status, String reason) {
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException("审核状态只能是同意或拒绝");
        }
        MerchantApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("申请记录不存在");
        }
        if (!Integer.valueOf(0).equals(application.getStatus())) {
            throw new BusinessException("该申请已处理");
        }

        SysUser user = userMapper.selectById(application.getUserId());
        if (user == null) {
            throw new BusinessException("申请用户不存在");
        }

        application.setStatus(status);
        application.setReviewerId(operatorId);
        application.setReason(status == 2 ? (StringUtils.hasText(reason) ? reason : "不满足商户入驻要求") : null);

        if (status == 1) {
            addMerchantRoleIfAbsent(user.getId());
            evictUserCacheAfterCommit(user.getId());
        }
        applicationMapper.updateById(application);

        return toApplicationVO(application);
    }

    @Override
    public MerchantApplicationVO getMerchantApplicationById(Long operatorId, Long applicationId) {
        MerchantApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException("申请记录不存在");
        }
        return toApplicationVO(application);
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }
        if (CommonConstant.ROLE_USER.equals(role)
                || CommonConstant.ROLE_MERCHANT.equals(role)
                || CommonConstant.ROLE_ADMIN.equals(role)) {
            return role;
        }
        throw new BusinessException("角色类型不正确");
    }

    private void addMerchantRoleIfAbsent(Long userId) {
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        if (roles.contains(CommonConstant.ROLE_MERCHANT)) {
            return;
        }
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, CommonConstant.ROLE_MERCHANT));
        if (role == null) {
            throw new BusinessException("商户角色未初始化");
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    private MerchantApplicationVO toApplicationVO(MerchantApplication application) {
        SysUser user = userMapper.selectById(application.getUserId());
        return MerchantApplicationVO.of(
                application.getId(),
                application.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getNickname(),
                user == null ? null : user.getEmail(),
                application.getStatus(),
                application.getReason(),
                null,
                application.getCreateTime(),
                application.getUpdateTime());
    }

    //当事务提交以后清空redis缓存
    private void evictUserCacheAfterCommit(Long userId) {
        Runnable evictTask = () -> {
            Cache userInfoCache = cacheManager.getCache(USER_INFO_CACHE);
            if (userInfoCache != null) {
                userInfoCache.evict(userId);
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictTask.run();
                }
            });
            return;
        }
        evictTask.run();
    }

}
