package com.nursing.catalog.repository;

import com.nursing.catalog.dto.response.ServiceSpecResponse;
import com.nursing.catalog.entity.ServiceSpec;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceSpecMapper {
    List<ServiceSpecResponse> selectByItemId(@Param("serviceItemId") Long serviceItemId);

    List<ServiceSpecResponse> selectByItemIds(@Param("serviceItemIds") List<Long> serviceItemIds);

    ServiceSpec selectAdminById(@Param("id") Long id);

    List<ServiceSpec> selectAdminByItemId(@Param("serviceItemId") Long serviceItemId);

    int markDeletedByItemId(@Param("serviceItemId") Long serviceItemId);

    int insert(ServiceSpec spec);

    int update(ServiceSpec spec);

    int updateStatus(@Param("id") Long id, @Param("status") int status);
}
