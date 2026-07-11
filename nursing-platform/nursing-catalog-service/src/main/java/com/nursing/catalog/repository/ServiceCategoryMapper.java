package com.nursing.catalog.repository;

import com.nursing.catalog.entity.ServiceCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceCategoryMapper {
    List<ServiceCategory> selectListVisible();

    ServiceCategory selectVisibleById(@Param("id") Long id);

    List<Long> selectVisibleDescendantIds(@Param("pathPrefix") String pathPrefix);
}
