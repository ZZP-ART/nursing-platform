package com.nursing.operations.service;

import com.nursing.common.constant.ApiCode;
import com.nursing.common.exception.BusinessException;
import com.nursing.common.util.SnowflakeIdWorker;
import com.nursing.operations.dto.request.CaregiverApplicationRequest;
import com.nursing.operations.entity.CaregiverApplication;
import com.nursing.operations.feign.UserServiceFeignClient;
import com.nursing.operations.repository.CaregiverApplicationMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaregiverApplicationService {
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_REJECTED = 2;

    private final CaregiverApplicationMapper mapper;
    private final UserServiceFeignClient users;
    private final SnowflakeIdWorker ids;
    private final String internalToken;

    public CaregiverApplicationService(CaregiverApplicationMapper mapper,
                                       UserServiceFeignClient users,
                                       SnowflakeIdWorker ids,
                                       @Value("${nursing.internal.token:}") String internalToken) {
        this.mapper = mapper;
        this.users = users;
        this.ids = ids;
        this.internalToken = internalToken;
    }

    @Transactional
    public CaregiverApplication apply(Long userId, CaregiverApplicationRequest request) {
        CaregiverApplication existing = mapper.selectByUserId(userId);
        if (existing != null) {
            if (!Integer.valueOf(STATUS_REJECTED).equals(existing.getStatus())) {
                throw new BusinessException(ApiCode.CONFLICT, "Caregiver application already exists");
            }
            if (mapper.resubmitRejected(userId, request) == 0) {
                throw new BusinessException(ApiCode.CONFLICT, "Application cannot be resubmitted");
            }
            return mapper.selectByUserId(userId);
        }

        CaregiverApplication application = new CaregiverApplication();
        LocalDateTime now = LocalDateTime.now();
        application.setId(ids.nextId());
        application.setUserId(userId);
        application.setRealName(request.getRealName());
        application.setPhone(request.getPhone());
        application.setServiceDistrict(request.getServiceDistrict());
        application.setSkills(request.getSkills());
        application.setStatus(STATUS_PENDING);
        application.setCreateTime(now);
        application.setUpdateTime(now);
        mapper.insert(application);
        return application;
    }

    public CaregiverApplication getByUserId(Long userId) {
        return mapper.selectByUserId(userId);
    }

    public List<CaregiverApplication> pending() {
        return mapper.selectPending();
    }

    @Transactional
    public void approve(Long userId, String remark) {
        if (mapper.approve(userId, remark) == 0) {
            throw new BusinessException(ApiCode.CONFLICT, "Application does not exist or was already handled");
        }
        users.grantRole(userId, "CAREGIVER", internalToken);
    }

    @Transactional
    public void reject(Long userId, String remark) {
        if (mapper.reject(userId, remark) == 0) {
            throw new BusinessException(ApiCode.CONFLICT, "Application does not exist or was already handled");
        }
    }
}
