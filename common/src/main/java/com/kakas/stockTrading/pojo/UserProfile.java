package com.kakas.stockTrading.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class UserProfile {
    /**
     * 关联至用户ID.
     */
    @TableId(type= IdType.AUTO)
    private Long userId;

    /**
     * 登录Email
     */
    private String email;

    private String name;

    private Long createdAt;

    private Long updatedAt;

    public static UserProfile createUserProfile(String email, String name, Long createdAt) {
        UserProfile userProfile = new UserProfile();
        userProfile.setEmail(email);
        userProfile.setName(name);
        userProfile.setCreatedAt(createdAt);
        userProfile.setUpdatedAt(createdAt);
        return userProfile;
    }
}
