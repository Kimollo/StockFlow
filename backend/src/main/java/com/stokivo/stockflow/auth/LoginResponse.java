package com.stokivo.stockflow.auth;

import com.stokivo.stockflow.user.UserResponse;

public record LoginResponse(String token, UserResponse user) {
}
