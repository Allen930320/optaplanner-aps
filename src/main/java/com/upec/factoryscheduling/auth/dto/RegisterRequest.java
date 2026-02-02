package com.upec.factoryscheduling.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * 注册请求 DTO
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度必须在4-50个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少为6个字符")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;
}
