package com.shoping.module.user.vo;

import com.shoping.module.user.entity.Member;
import com.shoping.module.user.entity.User;
import lombok.Data;

@Data
public class UserVO {

    private Long id;
    private String phone;
    private String nickname;
    private String avatar;
    private Integer status;
    private Integer level;
    private Long growthValue;
    private Integer points;

    public static UserVO from(User user, Member member) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        if (member != null) {
            vo.setLevel(member.getLevel());
            vo.setGrowthValue(member.getGrowthValue());
            vo.setPoints(member.getPoints());
        }
        return vo;
    }
}
