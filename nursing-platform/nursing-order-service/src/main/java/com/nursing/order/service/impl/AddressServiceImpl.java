package com.nursing.order.service.impl;

import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.dto.request.AddressRequest;
import com.nursing.order.dto.response.AddressResponse;
import com.nursing.order.entity.UserAddress;
import com.nursing.order.repository.UserAddressMapper;
import com.nursing.order.service.IAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl implements IAddressService {
    private static final int ADDRESS_NOT_FOUND = 3013;

    private final UserAddressMapper userAddressMapper;
    private final SnowflakeIdWorker snowflakeIdWorker;

    public AddressServiceImpl(UserAddressMapper userAddressMapper, SnowflakeIdWorker snowflakeIdWorker) {
        this.userAddressMapper = userAddressMapper;
        this.snowflakeIdWorker = snowflakeIdWorker;
    }

    @Override
    public List<AddressResponse> listAddresses(Long userId) {
        return userAddressMapper.selectByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public Long createAddress(Long userId, AddressRequest request) {
        if (Integer.valueOf(1).equals(request.getIsDefault())) {
            userAddressMapper.clearDefault(userId);
        }
        UserAddress address = new UserAddress();
        address.setId(snowflakeIdWorker.nextId());
        address.setUserId(userId);
        copyRequest(request, address);
        address.setIsDefault(request.getIsDefault() == null ? 0 : request.getIsDefault());
        address.setIsDeleted(0);
        userAddressMapper.insert(address);
        return address.getId();
    }

    @Override
    @Transactional
    public void updateAddress(Long userId, Long addressId, AddressRequest request) {
        ensureAddress(userId, addressId);
        if (Integer.valueOf(1).equals(request.getIsDefault())) {
            userAddressMapper.clearDefault(userId);
        }
        UserAddress address = new UserAddress();
        address.setId(addressId);
        address.setUserId(userId);
        copyRequest(request, address);
        userAddressMapper.updateByIdAndUserId(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        ensureAddress(userId, addressId);
        userAddressMapper.logicalDelete(addressId, userId);
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        ensureAddress(userId, addressId);
        userAddressMapper.clearDefault(userId);
        userAddressMapper.setDefault(addressId, userId);
    }

    private void ensureAddress(Long userId, Long addressId) {
        if (userAddressMapper.selectByIdAndUserId(addressId, userId) == null) {
            throw new BusinessException(ADDRESS_NOT_FOUND, "地址不存在");
        }
    }

    private void copyRequest(AddressRequest request, UserAddress address) {
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setTag(request.getTag());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetailAddress(request.getDetailAddress());
        address.setIsDefault(request.getIsDefault());
    }

    private AddressResponse toResponse(UserAddress address) {
        return new AddressResponse(
                address.getId(),
                address.getReceiverName(),
                address.getReceiverPhone(),
                address.getTag(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetailAddress(),
                address.getIsDefault());
    }
}
