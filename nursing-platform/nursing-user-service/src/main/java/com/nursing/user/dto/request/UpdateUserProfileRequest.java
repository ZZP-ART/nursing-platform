package com.nursing.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class UpdateUserProfileRequest {

    @Size(min = 2, max = 16, message = "昵称长度需为2-16个字符")
    private String nickname;

    @Size(max = 256, message = "头像URL长度不能超过256个字符")
    private String avatar;

    @Min(value = 0, message = "性别参数不正确")
    @Max(value = 2, message = "性别参数不正确")
    private Integer gender;

    @Size(min = 18, max = 18, message = "身份证号格式不正确")
    private String idCard;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }
}
