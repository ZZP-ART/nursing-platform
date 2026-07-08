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
}
