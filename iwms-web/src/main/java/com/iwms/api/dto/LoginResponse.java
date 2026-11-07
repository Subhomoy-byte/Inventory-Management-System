package com.iwms.api.dto;

public record LoginResponse(
        String token,
        UserDto user
) {
}
