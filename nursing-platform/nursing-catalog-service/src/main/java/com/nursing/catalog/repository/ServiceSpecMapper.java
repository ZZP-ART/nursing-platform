package com.nursing.catalog.repository;

import com.nursing.catalog.dto.vo.SpecVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceSpecMapper {
    List<SpecVO> selectByItemId(@Param("serviceItemId") Long serviceItemId);

    List<SpecVO> selectByItemIds(@Param("serviceItemIds") List<Long> serviceItemIds);
}
