package com.nursing.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserRoleMapper {
    int insertIgnore(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    List<String> selectRoleCodes(@Param("userId") Long userId);
}
