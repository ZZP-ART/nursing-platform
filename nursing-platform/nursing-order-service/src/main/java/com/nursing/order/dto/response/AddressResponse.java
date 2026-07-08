package com.nursing.order.dto.response;

public record AddressResponse(
        Long addressId,
        String receiverName,
        String receiverPhone,
        String tag,
        String province,
        String city,
        String district,
        String detailAddress,
        Integer isDefault) {
}
