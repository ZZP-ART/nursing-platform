package com.nursing.feedback.repository;

import com.nursing.feedback.entity.ComplaintTrack;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ComplaintTrackMapper {
    int insert(ComplaintTrack track);

    List<ComplaintTrack> selectByComplaintId(@Param("complaintId") Long complaintId);
}
