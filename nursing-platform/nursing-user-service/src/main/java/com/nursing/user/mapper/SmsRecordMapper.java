package com.nursing.user.mapper;

import com.nursing.user.entity.SmsRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SmsRecordMapper {

    int insert(SmsRecord smsRecord);

    int countTodayByPhone(@Param("phone") String phone);
}
