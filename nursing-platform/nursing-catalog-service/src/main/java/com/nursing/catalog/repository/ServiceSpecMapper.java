package com.nursing.catalog.repository;

import com.nursing.catalog.dto.response.ServiceSpecResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceSpecMapper {
    List<ServiceSpecResponse> selectByItemId(@Param("serviceItemId") Long serviceItemId);

    List<ServiceSpecResponse> selectByItemIds(@Param("serviceItemIds") List<Long> serviceItemIds);
}
