package com.nursing.catalog.repository;

import com.nursing.catalog.dto.vo.ItemDetailVO;
import com.nursing.catalog.dto.vo.ItemPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceItemMapper {
    List<ItemPageVO> selectPage(@Param("categoryId") Long categoryId,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    long count(@Param("categoryId") Long categoryId);

    ItemDetailVO selectById(@Param("id") Long id);

    List<ItemPageVO> searchPage(@Param("keyword") String keyword,
                                @Param("categoryId") Long categoryId,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    long searchCount(@Param("keyword") String keyword,
                     @Param("categoryId") Long categoryId);
}
