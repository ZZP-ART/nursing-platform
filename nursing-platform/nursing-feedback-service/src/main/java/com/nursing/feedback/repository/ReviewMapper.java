package com.nursing.feedback.repository;

import com.nursing.feedback.entity.Review;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewMapper {
    int insert(Review review);

    Review selectById(@Param("id") Long id);

    Review selectByOrderId(@Param("orderId") Long orderId);

    List<Review> selectApprovedPageByItemId(@Param("serviceItemId") Long serviceItemId,
                                            @Param("offset") int offset,
                                            @Param("size") int size);

    long countApprovedByItemId(@Param("serviceItemId") Long serviceItemId);
    List<Review> selectMissingSnapshots(@Param("limit") int limit);
    int updateSnapshots(@Param("id") Long id, @Param("serviceItemName") String serviceItemName, @Param("specName") String specName);
    List<Review> selectAdminPage(@Param("status") Integer status,
                                 @Param("offset") int offset,
                                 @Param("size") int size);
    long countAdmin(@Param("status") Integer status);
    int updateStatus(@Param("id") Long id, @Param("status") int status);
}
