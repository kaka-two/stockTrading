package com.kakas.stockTrading.dbService;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakas.stockTrading.mapper.PasswordAuthMapper;
import com.kakas.stockTrading.mapper.UserProfileMapper;
import com.kakas.stockTrading.pojo.PasswordAuth;
import com.kakas.stockTrading.pojo.UserProfile;
import com.kakas.stockTrading.util.RandomStringUtil;
import com.kakas.stockTrading.util.SignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.commons.*;

@Component
@Slf4j
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile>{
    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private PasswordAuthServiceImpl passwordAuthService;

    public UserProfile getUserProfile(Long userId) {
        QueryWrapper<UserProfile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).last("limit 1");
        return userProfileMapper.selectOne(queryWrapper);
    }

    public UserProfile getUserProfileByEmail(String email) {
        QueryWrapper<UserProfile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email).last("limit 1");
        return userProfileMapper.selectOne(queryWrapper);
    }

    public UserProfile signup(String name, String email, String password) {
        final Long ts = System.currentTimeMillis();
        // 插入user_profile表
        UserProfile userProfile = UserProfile.createUserProfile(email, name, ts);
        userProfileMapper.insert(userProfile);
        // 插入password_auth表
        String random = RandomStringUtil.getRandomString(32);
        String passwd = SignatureUtil.sign(password, random);
        PasswordAuth passwordAuth = PasswordAuth.createPasswordAuth(userProfile.getUserId(), random, passwd);
        passwordAuthService.save(passwordAuth);
        return userProfile;
    }

    public UserProfile signIn(String email, String password) {
        UserProfile userProfile = this.getUserProfileByEmail(email);
        if (userProfile == null) {
            log.error("unregistered email : {}", email);
        }
        // 校验hmacsha256
        QueryWrapper<PasswordAuth> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userProfile.getUserId()).last("limit 1");
        PasswordAuth passwordAuth = passwordAuthService.getOne(queryWrapper);
        if (!SignatureUtil.valid(password, passwordAuth.getRandom(), passwordAuth.getPasswd())) {
            log.error("Password error, email: {}, password: {}", email, password);
            return null;
        }
        return userProfile;
    }
}
