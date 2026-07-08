package com.nursing.order.repository;

import com.nursing.order.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserAddressMapper {
    int insert(UserAddress address);

    List<UserAddress> selectByUserId(@Param("userId") Long userId);

    UserAddress selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int clearDefault(@Param("userId") Long userId);

    int updateByIdAndUserId(UserAddress address);

    int logicalDelete(@Param("id") Long id, @Param("userId") Long userId);

    int setDefault(@Param("id") Long id, @Param("userId") Long userId);
}
