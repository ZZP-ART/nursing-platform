package com.nursing.user.mapper;

import com.nursing.user.entity.SmsRecordStatusTransition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SmsRecordStatusTransitionMapper {
    int insert(SmsRecordStatusTransition transition);
}
