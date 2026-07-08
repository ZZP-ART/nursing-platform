package com.nursing.order.service;

import com.nursing.order.dto.request.AddressRequest;
import com.nursing.order.dto.response.AddressResponse;

import java.util.List;

public interface IAddressService {
    List<AddressResponse> listAddresses(Long userId);

    Long createAddress(Long userId, AddressRequest request);

    void updateAddress(Long userId, Long addressId, AddressRequest request);

    void deleteAddress(Long userId, Long addressId);

    void setDefaultAddress(Long userId, Long addressId);
}
