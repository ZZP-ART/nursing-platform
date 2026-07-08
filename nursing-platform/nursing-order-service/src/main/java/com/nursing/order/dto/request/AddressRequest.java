package com.nursing.order.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AddressRequest {
    public interface Create {}
    public interface Update {}

    @NotBlank(groups = Create.class, message = "收件人姓名不能为空")
    @Size(min = 2, max = 16, groups = {Create.class, Update.class}, message = "收件人姓名需为2-16个字符")
    private String receiverName;

    @NotBlank(groups = Create.class, message = "联系电话不能为空")
    @Pattern(regexp = "^1\\d{10}$", groups = {Create.class, Update.class}, message = "联系电话格式不正确")
    private String receiverPhone;

    @NotBlank(groups = Create.class, message = "地址标签不能为空")
    @Pattern(regexp = "家|公司|学校|其他", groups = {Create.class, Update.class}, message = "地址标签不合法")
    private String tag;

    @NotBlank(groups = Create.class, message = "省份不能为空")
    @Size(max = 32, groups = {Create.class, Update.class}, message = "省份最多32个字符")
    private String province;

    @NotBlank(groups = Create.class, message = "城市不能为空")
    @Size(max = 32, groups = {Create.class, Update.class}, message = "城市最多32个字符")
    private String city;

    @NotBlank(groups = Create.class, message = "区县不能为空")
    @Size(max = 32, groups = {Create.class, Update.class}, message = "区县最多32个字符")
    private String district;

    @NotBlank(groups = Create.class, message = "详细地址不能为空")
    @Size(min = 5, max = 100, groups = {Create.class, Update.class}, message = "详细地址需为5-100个字符")
    private String detailAddress;

    @Min(value = 0, groups = {Create.class, Update.class}, message = "默认地址标记不合法")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "默认地址标记不合法")
    private Integer isDefault;

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
}
