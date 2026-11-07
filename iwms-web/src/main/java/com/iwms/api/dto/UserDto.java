package com.iwms.api.dto;

import java.util.List;

public record UserDto(
        String id,
        String username,
        String fullName,
        String role,
        List<String> permissions
) {
}
