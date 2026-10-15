package com.rental.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Set;
import java.util.List;
import javax.validation.constraints.*;

/** 请求 DTO 与数据库记录分开，客户端不能绑定状态、实收金额等受保护字段。 */
public final class Models {
    private Models() { }

    @Data public static class House {
        private Long id; private String code; private String title; private String address;
        private BigDecimal area; private BigDecimal monthlyRent; private BigDecimal deposit;
        private String status; private int version; private String createdAt;
    }
    @Data public static class HouseInput {
        @NotBlank @Size(max=32) @Pattern(regexp="[A-Za-z0-9_-]+") private String code;
        @NotBlank @Size(max=80) private String title;
        @NotBlank @Size(max=200) private String address;
        @NotNull @DecimalMin("1.00") @DecimalMax("9999.99") @Digits(integer=4,fraction=2) private BigDecimal area;
        @NotNull @DecimalMin("0.01") @DecimalMax("9999999.99") @Digits(integer=7,fraction=2) private BigDecimal monthlyRent;
        @NotNull @DecimalMin("0.00") @DecimalMax("9999999.99") @Digits(integer=7,fraction=2) private BigDecimal deposit;
        @NotBlank @Pattern(regexp="VACANT|OFFLINE") private String status;
        @Min(0) private int version;
    }
    @Data public static class Tenant {
        private Long id; private String name; private String phone; private String email;
        private String remark; private String createdAt;
    }
    @Data public static class TenantInput {
        @NotBlank @Size(max=40) private String name;
        @NotBlank @Pattern(regexp="1[3-9][0-9]{9}") private String phone;
        @Email @Size(max=100) private String email;
        @Size(max=200) private String remark;
    }
    @Data public static class Contract {
        private Long id; private String contractNo; private Long houseId; private Long tenantId;
        private String houseTitle; private String tenantName; private String startDate; private String endDate;
        private int months; private BigDecimal monthlyRent; private BigDecimal deposit;
        private String status; private String closedAt; private String createdAt;
    }
    @Data public static class ContractInput {
        @NotNull @Positive private Long houseId;
        @NotNull @Positive private Long tenantId;
        @NotBlank @Pattern(regexp="[0-9]{4}-[0-9]{2}-[0-9]{2}") private String startDate;
        @Min(1) @Max(24) private int months;
    }
    @Data public static class Bill {
        private Long id; private Long contractId; private String contractNo; private String houseTitle;
        private String tenantName; private String period; private String dueDate;
        private BigDecimal amount; private BigDecimal paidAmount; private String status;
    }
    @Data public static class Payment {
        private Long id; private Long billId; private BigDecimal amount;
        private String requestId; private String method; private String note; private String createdAt; private String actor;
    }
    @Data public static class PaymentInput {
        @NotNull @DecimalMin("0.01") @DecimalMax("9999999.99") @Digits(integer=7,fraction=2) private BigDecimal amount;
        @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{8,64}") private String requestId;
        @NotBlank @Pattern(regexp="BANK|CASH|OTHER") private String method;
        @Size(max=200) private String note;
    }
    @Data public static class LoginInput {
        @NotBlank @Pattern(regexp="[A-Za-z0-9_]{5,30}") private String username;
        @NotBlank @Size(min=6,max=64) private String password;
    }
    @Data public static class Principal {
        private Long id; private String username; private String nickname;
        private Set<String> roles; private Set<String> permissions;
    }
    /** 仅供认证层使用的数据库记录，不作为任何响应返回。 */
    @Data public static class AuthRow {
        private Long id; private String username; private String nickname; private String password; private String status; private String delFlag;
    }
    @Data public static class UserView {
        private Long id; private String username; private String nickname; private String email; private String phone; private String status;
        private List<String> roles;
    }
    @Data public static class UserInput {
        @NotBlank @Pattern(regexp="[A-Za-z0-9_]{5,30}") private String username;
        @NotBlank @Size(max=30) private String nickname;
        @Email @Size(max=50) private String email;
        @Pattern(regexp="1[3-9][0-9]{9}|^$") private String phone;
        @NotBlank @Size(min=8,max=64) private String password;
        @NotBlank @Pattern(regexp="admin|operator|viewer") private String role;
    }
    @Data public static class UserAccessInput {
        @NotBlank @Pattern(regexp="admin|operator|viewer") private String role;
        @NotBlank @Pattern(regexp="0|1") private String status;
    }
    @Data public static class RoleView {
        private Long id; private String name; private String tag; private Set<String> permissions;
    }
    @Data public static class Audit {
        private Long id; private String actor; private String action; private String target;
        private String requestId; private String createdAt;
    }
}
