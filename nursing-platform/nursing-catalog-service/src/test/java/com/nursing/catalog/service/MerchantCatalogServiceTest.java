package com.nursing.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nursing.catalog.entity.ServiceCategory;
import com.nursing.catalog.entity.ServiceItem;
import com.nursing.catalog.entity.ServiceSpec;
import com.nursing.catalog.repository.ServiceCategoryMapper;
import com.nursing.catalog.repository.ServiceItemMapper;
import com.nursing.catalog.repository.ServiceSpecMapper;
import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantCatalogServiceTest {
    @Mock private ServiceCategoryMapper categoryMapper;
    @Mock private ServiceItemMapper itemMapper;
    @Mock private ServiceSpecMapper specMapper;
    @Mock private SnowflakeIdWorker idWorker;
    @InjectMocks private MerchantCatalogService merchantCatalogService;

    @Test
    void publishesOnlyAnApprovedServiceOwnedByTheCurrentMerchant() {
        ServiceItem item = item(9001L, "APPROVED", "OFFLINE");
        when(itemMapper.selectByOwnerUserIdAndId(10001L, 9001L)).thenReturn(item);
        when(categoryMapper.selectById(101L)).thenReturn(category());
        when(specMapper.selectAdminByItemId(9001L)).thenReturn(List.of(spec()));

        var response = merchantCatalogService.publish(10001L, 9001L);

        assertThat(response.getPublishStatus()).isEqualTo("PUBLISHED");
        assertThat(item.getStatus()).isEqualTo(1);
        assertThat(item.getPublishStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void rejectsPublicationBeforeSubmission() {
        ServiceItem item = item(9001L, "DRAFT", "OFFLINE");
        when(itemMapper.selectByOwnerUserIdAndId(10001L, 9001L)).thenReturn(item);

        assertThatThrownBy(() -> merchantCatalogService.publish(10001L, 9001L))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.getCode()).isEqualTo(ApiCode.BIZ_ERROR);
                });
    }

    private ServiceItem item(Long id, String auditStatus, String publishStatus) {
        ServiceItem item = new ServiceItem();
        item.setId(id);
        item.setOwnerUserId(10001L);
        item.setCategoryId(101L);
        item.setName("上门护理");
        item.setAuditStatus(auditStatus);
        item.setPublishStatus(publishStatus);
        item.setStatus(0);
        item.setVersion(1);
        return item;
    }

    private ServiceCategory category() {
        ServiceCategory category = new ServiceCategory();
        category.setId(101L);
        category.setName("专业护理");
        category.setStatus(1);
        return category;
    }

    private ServiceSpec spec() {
        ServiceSpec spec = new ServiceSpec();
        spec.setId(1L);
        spec.setName("单次服务");
        spec.setPrice(BigDecimal.valueOf(100));
        spec.setOriginalPrice(BigDecimal.valueOf(120));
        spec.setDuration(60);
        spec.setStatus(1);
        return spec;
    }
}
