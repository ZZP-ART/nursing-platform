package com.nursing.feedback.repository;

import com.nursing.feedback.entity.ReviewImage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewImageMapper {
    int insertBatch(@Param("images") List<ReviewImage> images);

    List<ReviewImage> selectByReviewId(@Param("reviewId") Long reviewId);

    List<ReviewImage> selectByReviewIds(@Param("reviewIds") List<Long> reviewIds);
}
