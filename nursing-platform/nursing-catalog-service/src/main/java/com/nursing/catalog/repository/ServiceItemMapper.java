package com.nursing.catalog.repository;

import com.nursing.catalog.dto.response.ItemDetailResponse;
import com.nursing.catalog.dto.response.ItemListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ServiceItemMapper {
    List<ItemListResponse> selectPage(@Param("categoryIds") List<Long> categoryIds,
                                @Param("cursorSortOrder") Integer cursorSortOrder,
                                @Param("cursorId") Long cursorId,
                                @Param("limit") int limit);

    ItemDetailResponse selectById(@Param("id") Long id);

    List<ItemListResponse> searchPage(@Param("keyword") String keyword,
                                @Param("categoryIds") List<Long> categoryIds,
                                @Param("cursorSortOrder") Integer cursorSortOrder,
                                @Param("cursorId") Long cursorId,
                                @Param("limit") int limit);
}
