package com.nursing.order.service.impl;

import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.order.dto.request.AddressRequest;
import com.nursing.order.entity.UserAddress;
import com.nursing.order.repository.UserAddressMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AddressServiceImplTest {

    @Test
    void createDefaultAddressClearsExistingDefaultBeforeInsert() {
        UserAddressMapper mapper = mock(UserAddressMapper.class);
        AddressServiceImpl service = new AddressServiceImpl(mapper, new SnowflakeIdWorker(1, 1));
        AddressRequest request = new AddressRequest();
        request.setReceiverName("Alice");
        request.setReceiverPhone("13812345678");
        request.setTag("家");
        request.setProvince("北京市");
        request.setCity("北京市");
        request.setDistrict("朝阳区");
        request.setDetailAddress("建国路 88 号 6 栋 301");
        request.setIsDefault(1);

        Long addressId = service.createAddress(10001L, request);

        verify(mapper).clearDefault(10001L);
        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(mapper).insert(captor.capture());
        UserAddress inserted = captor.getValue();
        assertThat(inserted.getId()).isEqualTo(addressId);
        assertThat(inserted.getUserId()).isEqualTo(10001L);
        assertThat(inserted.getIsDefault()).isEqualTo(1);
        assertThat(inserted.getIsDeleted()).isZero();
    }
}
