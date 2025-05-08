// app/ui/viewmodel/AccountSettingsViewModel.java
package com.owlerdev.owler.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;

import com.owlerdev.owler.model.user.User;
import com.owlerdev.owler.service.AuthService;
import com.owlerdev.owler.service.UserService;
import com.owlerdev.owler.utils.Result;

import java.util.Map;

import javax.inject.Inject;

@HiltViewModel
public class AccountSettingsViewModel extends ViewModel {
    private final UserService userService;
    private final AuthService authService;
    private final MutableLiveData<Result<User>> googleCallbackResult = new MutableLiveData<>();

    @Inject
    public AccountSettingsViewModel(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }



    public LiveData<Result<User>> updateProfile(Map<String,Object> updates) {

        return userService.updateProfile(updates);
    }
    public LiveData<Result<Void>> changePassword(String oldPassword, String newPassword) {
        return userService.changePassword(oldPassword, newPassword);
    }
    public LiveData<Result<Void>> deleteAccount(String Password) {
        return userService.deleteAccount(Password);
    }
    public LiveData<Result<Void>> logout() {
        return authService.logout();
    }
    public LiveData<Result<User>> getCachedUser() {
        return userService.getCachedUser();
    }
}