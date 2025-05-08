// app/service/AuthService.java
package com.owlerdev.owler.service;

import androidx.lifecycle.LiveData;

import com.owlerdev.owler.data.local.TokenManager;
import com.owlerdev.owler.data.repository.AuthRepository;
import com.owlerdev.owler.model.user.User;
import com.owlerdev.owler.utils.Result;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service layer for authentication flows: email/password, Google OAuth2,
 * token refresh, and logout. Delegates to AuthRepository.
 */
@Singleton
public class AuthService {
    private final AuthRepository authRepository;
    private final TokenManager tokenManager;

    @Inject
    public AuthService(AuthRepository authRepository, TokenManager tokenManager) {
        this.authRepository = authRepository;
        this.tokenManager = tokenManager;
    }

    /**
     * Register a new user with email/password.
     */
    public LiveData<Result<User>> register(
            String email,
            String password,
            String name,
            int age,
            String gender
    ) {
        return authRepository.register(email, password, name, age, gender);
    }

    /**
     * Login with email/password.
     */
    public LiveData<Result<User>> login(
            String email,
            String password
    ) {
        return authRepository.login(email, password);
    }




    public LiveData<Result<User>> googleSignin(String code) {
        return authRepository.googleSignin(code);
    }




    /**
     * Logout the current user, clearing tokens and cached data.
     */
    public LiveData<Result<Void>> logout() {
        return authRepository.logout();

    }

}
