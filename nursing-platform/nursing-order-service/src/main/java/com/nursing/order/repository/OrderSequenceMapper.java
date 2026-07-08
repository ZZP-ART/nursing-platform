package com.nursing.order.repository;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderSequenceMapper {
    int nextSequence();

    Long lastInsertId();
}
