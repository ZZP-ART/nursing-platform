package com.nursing.feedback.repository;

import com.nursing.feedback.entity.Complaint;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ComplaintMapper {
    int insert(Complaint complaint);

    Complaint selectById(@Param("id") Long id);

    Complaint selectByUserAndIdempotentKey(@Param("userId") Long userId,
                                           @Param("idempotentKey") String idempotentKey);

    List<Complaint> selectPageByUserId(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    long countByUserId(@Param("userId") Long userId);

    List<Complaint> selectAdminPage(@Param("status") Integer status,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    long countAdmin(@Param("status") Integer status);

    int updateStatus(@Param("id") Long id, @Param("status") int status);
}
