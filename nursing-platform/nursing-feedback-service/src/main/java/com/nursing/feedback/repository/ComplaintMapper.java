package com.nursing.feedback.repository;

import com.nursing.feedback.entity.Complaint;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ComplaintMapper {
    int insert(Complaint complaint);

    Complaint selectById(@Param("id") Long id);

    Complaint selectByIdempotentKey(@Param("idempotentKey") String idempotentKey);

    List<Complaint> selectPageByUserId(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    long countByUserId(@Param("userId") Long userId);
}
