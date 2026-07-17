package com.nursing.catalog.repository;

import com.nursing.catalog.dto.response.ItemDetailResponse;
import com.nursing.catalog.dto.response.ItemListResponse;
import com.nursing.catalog.entity.ServiceItem;
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

    ServiceItem selectAdminById(@Param("id") Long id);

    List<ServiceItem> selectByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    ServiceItem selectByOwnerUserIdAndId(@Param("ownerUserId") Long ownerUserId, @Param("id") Long id);

    int insert(ServiceItem item);

    int update(ServiceItem item);

    int updateStatus(@Param("id") Long id, @Param("status") int status);
}
