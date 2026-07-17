package com.nursing.user.mapper;

import com.nursing.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {

    int insert(User user);

    User selectById(@Param("id") Long id);

    User selectByPhone(@Param("phone") String phone);

    int updateById(User user);

    int updateProfileByIdAndVersion(User user);

    int updatePassword(@Param("phone") String phone,
                       @Param("password") String password,
                       @Param("updateTime") LocalDateTime updateTime);

    int updateLastLoginTime(@Param("id") Long id,
                            @Param("lastLoginTime") LocalDateTime lastLoginTime);

    int incrementAuthorizationVersion(@Param("id") Long id);
}
