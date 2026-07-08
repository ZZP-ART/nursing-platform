package com.nursing.user.mapper;

import com.nursing.user.entity.UserToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserTokenMapper {

    int insert(UserToken userToken);

    List<UserToken> selectByUserId(@Param("userId") Long userId);
}
