package com.nursing.user.mapper;

import com.nursing.user.entity.FileUploadRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileUploadRecordMapper {
    int insert(FileUploadRecord record);

    FileUploadRecord selectById(@Param("id") Long id);

    FileUploadRecord selectByUserBizHash(@Param("userId") Long userId,
                                         @Param("bizType") String bizType,
                                         @Param("fileHash") String fileHash,
                                         @Param("fileExt") String fileExt);
}
