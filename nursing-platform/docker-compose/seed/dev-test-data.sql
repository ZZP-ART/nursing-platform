-- Development/test-only data. Run only through the explicit Compose seed profile.
-- The fixed credentials below are intentionally restricted to dev/test environments.

USE user_db;
INSERT INTO user (id,phone,password,nickname,gender,status,is_deleted,version,authorization_version)
VALUES
 (90001,'admin','$2b$10$o.tMKEwbL7K1Q5sS74UODuQ/K.IYnM7Qmm2o/LONx2l8AUMvtOEyq','平台管理员',0,0,0,0,1),
 (10001,'13800138000','$2b$10$YRLswLvAJZIl8rR/iXR1W.ZwDcPKJi9icWKPN4fyajjWHzItXx1hu','安心顾客',2,0,0,0,1),
 (10002,'13800138001','$2b$10$YRLswLvAJZIl8rR/iXR1W.ZwDcPKJi9icWKPN4fyajjWHzItXx1hu','王护理员',2,0,0,0,1),
 (10003,'13800138002','$2b$10$YRLswLvAJZIl8rR/iXR1W.ZwDcPKJi9icWKPN4fyajjWHzItXx1hu','康宁护理中心',0,0,0,0,1),
 (10004,'13800138003','$2b$10$YRLswLvAJZIl8rR/iXR1W.ZwDcPKJi9icWKPN4fyajjWHzItXx1hu','赵敏',2,0,0,0,1),
 (10005,'13800138004','$2b$10$YRLswLvAJZIl8rR/iXR1W.ZwDcPKJi9icWKPN4fyajjWHzItXx1hu','陈丽',2,0,0,0,1)
ON DUPLICATE KEY UPDATE password=VALUES(password), nickname=VALUES(nickname), gender=VALUES(gender),
 status=0, is_deleted=0, authorization_version=authorization_version+1;

DELETE FROM user_role WHERE user_id IN (90001,10001,10002,10003,10004,10005);
INSERT INTO user_role (user_id,role_code) VALUES
 (90001,'ADMIN'), (10001,'CUSTOMER'), (10002,'CAREGIVER'), (10003,'MERCHANT_MEMBER'),
 (10004,'CUSTOMER'), (10005,'CUSTOMER');

USE operations_db;
INSERT INTO merchant_member (id,merchant_id,user_id,position,status) VALUES
 (21001,20001,10003,'OWNER',1)
ON DUPLICATE KEY UPDATE merchant_id=VALUES(merchant_id), position=VALUES(position), status=VALUES(status);
INSERT INTO merchant_caregiver (id,merchant_id,caregiver_user_id,status) VALUES
 (22001,20001,10002,1)
ON DUPLICATE KEY UPDATE status=VALUES(status);
INSERT INTO caregiver_profile (caregiver_id,user_id,real_name,service_areas,skills,audit_status,max_daily_orders,rating,completed_orders,status)
VALUES (50002,10002,'王芳','朝阳区、海淀区','生活照护、术后护理、老年康复',1,4,4.90,126,'AVAILABLE')
ON DUPLICATE KEY UPDATE real_name=VALUES(real_name), service_areas=VALUES(service_areas), skills=VALUES(skills),
 audit_status=VALUES(audit_status), max_daily_orders=VALUES(max_daily_orders), rating=VALUES(rating),
 completed_orders=VALUES(completed_orders), status=VALUES(status);
INSERT INTO caregiver_application (id,user_id,real_name,phone,service_district,skills,status,review_remark)
VALUES
 (71001,10002,'王芳','13800138001','北京市朝阳区、海淀区','生活照护、术后护理、老年康复',1,'资质核验通过'),
 (71002,10004,'赵敏','13800138003','北京市丰台区','生活照护、陪诊陪护',0,NULL),
 (71003,10005,'陈丽','13800138004','北京市东城区','康复护理',2,'请补充有效护理员资格证明')
ON DUPLICATE KEY UPDATE real_name=VALUES(real_name), phone=VALUES(phone), service_district=VALUES(service_district),
 skills=VALUES(skills), status=VALUES(status), review_remark=VALUES(review_remark);

DELETE action_record FROM service_action action_record
INNER JOIN service_assignment assignment_record ON assignment_record.id = action_record.assignment_id
WHERE assignment_record.order_id BETWEEN 31001 AND 31009;
DELETE FROM service_assignment WHERE order_id BETWEEN 31001 AND 31009;
INSERT INTO service_assignment (id,order_id,active_order_id,merchant_id,caregiver_user_id,status,remark,accepted_time)
VALUES
 (95003,31003,31003,20001,10002,0,'待护理人员接单',NULL),
 (95004,31004,31004,20001,10002,1,'已接单','2026-07-16 11:00:00'),
 (95005,31005,31005,20001,10002,1,'服务中','2026-07-16 09:00:00'),
 (95006,31006,31006,20001,10002,1,'待顾客确认','2026-07-15 09:00:00'),
 (95007,31007,31007,20001,10002,1,'已完成','2026-07-14 09:00:00');
INSERT INTO service_action (id,assignment_id,action,operator_user_id,remark,create_time) VALUES
 (97001,95005,'depart',10002,'预计30分钟后到达','2026-07-17 08:00:00'),
 (97002,95005,'check-in',10002,'已与家属确认身份','2026-07-17 08:30:00'),
 (97003,95005,'start',10002,'生命体征正常','2026-07-17 08:35:00'),
 (97004,95006,'depart',10002,'护理人员已出发','2026-07-16 12:30:00'),
 (97005,95006,'check-in',10002,'护理人员已签到','2026-07-16 13:00:00'),
 (97006,95006,'start',10002,'护理服务已开始','2026-07-16 13:05:00'),
 (97007,95006,'finish',10002,'护理服务已结束','2026-07-16 14:35:00'),
 (97008,95007,'depart',10002,'护理人员已出发','2026-07-15 08:00:00'),
 (97009,95007,'check-in',10002,'护理人员已签到','2026-07-15 08:30:00'),
 (97010,95007,'start',10002,'护理服务已开始','2026-07-15 08:35:00'),
 (97011,95007,'finish',10002,'护理服务已结束','2026-07-15 10:05:00');

USE order_db;
INSERT INTO user_address (id,user_id,receiver_name,receiver_phone,tag,province,city,district,detail_address,is_default,is_deleted)
VALUES
 (40001,10001,'张女士','13800138000','家','北京市','北京市','朝阳区','望京花园东区8号楼1202室',1,0),
 (40002,10001,'张先生','13800138000','公司','北京市','北京市','海淀区','中关村健康大厦A座806室',0,0)
ON DUPLICATE KEY UPDATE receiver_name=VALUES(receiver_name), receiver_phone=VALUES(receiver_phone), tag=VALUES(tag),
 province=VALUES(province), city=VALUES(city), district=VALUES(district), detail_address=VALUES(detail_address),
 is_default=VALUES(is_default), is_deleted=0;

DELETE FROM order_header WHERE id BETWEEN 31001 AND 31009;
INSERT INTO order_header (id,order_no,user_id,merchant_id,source,version,service_item_id,service_spec_id,service_item_name,category_name,spec_name,spec_price,spec_duration,quantity,catalog_snapshot_version,address_id,receiver_name,receiver_phone,address_detail,service_date,service_time_slot,total_amount,status,slot_occupied,remark,is_deleted,create_time)
VALUES
 (31001,'DEV-PAY-31001',10001,20001,0,0,201,301,'上门康复推拿','康复理疗','单次体验',198.00,60,1,1,40001,'张女士','13800138000','北京市北京市朝阳区望京花园东区8号楼1202室',DATE_ADD(CURDATE(),INTERVAL 30 DAY),'MORNING',198.00,0,1,'现场演示待支付订单',0,NOW()),
 (31002,'DEV-DISPATCH-31002',10001,20001,0,0,202,303,'术后康复护理','术后康复','单次护理',258.00,90,1,1,40001,'张女士','13800138000','北京市北京市朝阳区望京花园东区8号楼1202室',DATE_ADD(CURDATE(),INTERVAL 31 DAY),'AFTERNOON',258.00,1,1,'商户待派单',0,NOW()),
 (31003,'DEV-OFFER-31003',10001,20001,0,0,203,304,'老年康复训练','老年康复','单次训练',168.00,45,1,1,40001,'张女士','13800138000','北京市北京市朝阳区望京花园东区8号楼1202室',DATE_ADD(CURDATE(),INTERVAL 32 DAY),'MORNING',168.00,6,1,'老人行动不便，请提前联系',0,NOW()),
 (31004,'DEV-ACCEPT-31004',10001,20001,0,0,204,306,'常规体检套餐','上门体检','基础套餐',99.00,30,1,1,40001,'张女士','13800138000','北京市北京市朝阳区望京花园东区8号楼1202室',DATE_ADD(CURDATE(),INTERVAL 33 DAY),'AFTERNOON',99.00,7,1,'请提前电话联系',0,NOW()),
 (31005,'DEV-SERVICE-31005',10001,20001,0,0,206,309,'老人陪护服务','居家照护','日常陪护4小时',128.00,240,1,1,40001,'张女士','13800138000','北京市北京市朝阳区望京花园东区8号楼1202室',DATE_ADD(CURDATE(),INTERVAL 34 DAY),'MORNING',128.00,8,1,'服务进行中',0,NOW()),
 (31006,'DEV-CONFIRM-31006',10001,20001,0,0,201,301,'上门康复推拿','康复理疗','单次体验',198.00,60,1,1,40001,'张女士','13800138000','北京市北京市朝阳区望京花园东区8号楼1202室',DATE_SUB(CURDATE(),INTERVAL 30 DAY),'AFTERNOON',198.00,9,1,'待顾客确认',0,NOW()),
 (31007,'DEV-COMPLETE-31007',10001,20001,0,0,202,303,'术后康复护理','术后康复','单次护理',258.00,90,1,1,40001,'张女士','13800138000','北京市北京市朝阳区望京花园东区8号楼1202室',DATE_SUB(CURDATE(),INTERVAL 31 DAY),'MORNING',258.00,2,1,'已完成，可评价和投诉',0,NOW()),
 (31008,'DEV-CANCEL-31008',10001,20001,0,0,204,306,'常规体检套餐','上门体检','基础套餐',99.00,30,1,1,40001,'张女士','13800138000','北京市北京市朝阳区望京花园东区8号楼1202室',DATE_ADD(CURDATE(),INTERVAL 35 DAY),'MORNING',99.00,3,NULL,'已取消',0,NOW()),
 (31009,'DEV-REFUND-31009',10001,20001,0,0,201,301,'上门康复推拿','康复理疗','单次体验',198.00,60,1,1,40001,'张女士','13800138000','北京市北京市朝阳区望京花园东区8号楼1202室',DATE_SUB(CURDATE(),INTERVAL 32 DAY),'AFTERNOON',198.00,5,NULL,'已退款',0,NOW());
