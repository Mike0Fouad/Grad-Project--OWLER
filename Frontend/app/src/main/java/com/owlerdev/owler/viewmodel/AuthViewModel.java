// app/ui/viewmodel/AuthViewModel.java
package com.owlerdev.owler.viewmodel;

import android.content.Context;

import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import java.util.Arrays;
import java.util.List;

import com.owlerdev.owler.BuildConfig;
import com.owlerdev.owler.model.user.User;
import com.owlerdev.owler.service.AuthService;
import com.owlerdev.owler.utils.Result;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import timber.log.Timber;

@HiltViewModel
public class AuthViewModel extends ViewModel {
    private final AuthService authService;


    private final MutableLiveData<Result<User>> registerResult       = new MutableLiveData<>();
    private final MutableLiveData<Result<User>> loginResult          = new MutableLiveData<>();
    private final MutableLiveData<Result<User>> googleCallbackResult = new MutableLiveData<>();
    private static CredentialManager credentialManager;
    private static GetCredentialRequest googleRequest;
    @Inject
    public AuthViewModel(AuthService authService) {
        this.authService = authService;
    }

    public static GetCredentialRequest getGoogleRequest() {
        return googleRequest;
    }


    public LiveData<Result<User>> getRegisterResult()       { return registerResult; }
    public LiveData<Result<User>> getLoginResult()          { return loginResult; }
    public LiveData<Result<User>> getGoogleCallbackResult() { return googleCallbackResult; }

    public void register(String email, String password, String name, int age, String gender) {
        authService.register(email, password, name, age, gender)
                .observeForever(registerResult::postValue);
    }

    public LiveData<Result<User>> login(String email, String password) {
        authService.login(email, password)
                .observeForever(result -> {
                    if (result.isSuccess()) {
                        loginResult.postValue(result);
                    } else {
                        loginResult.postValue(Result.error(result.getError()));
                    }
                });
        return loginResult;
    }

    public LiveData<Result<User>> googleSignin(String idToken) {
        authService.googleSignin(idToken)
                .observeForever(result -> {
                    if (result.isSuccess()) {
                        googleCallbackResult.postValue(result);
                    } else {
                        googleCallbackResult.postValue(Result.error(result.getError()));
                    }
                });
        return googleCallbackResult;
    }
    public static CredentialManager setupCredentialManager(Context context) {
        CredentialManager credentialManager = CredentialManager.create(context);

        try {
            Timber.d("Setting up Google Sign-In with client ID: %s", BuildConfig.CLIENT_ID);
            List<String> fitScopes = Arrays.asList(
                    "https://www.googleapis.com/auth/fitness.activity.read",
                    "https://www.googleapis.com/auth/fitness.body.read"
            );
            // Build the Google ID token request option
            GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                    .setServerClientId(BuildConfig.CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .setNonce("random_nonce")
                    .setAutoSelectEnabled(false)
//                    .setScopes(fitScopes)
                    .build();



            googleRequest = new GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .setPreferImmediatelyAvailableCredentials(false)
                    .build();

            Timber.d("Google Sign-In setup completed successfully with options: filterByAuthorizedAccounts=false, autoSelectEnabled=false");
            return credentialManager;
        } catch (Exception e) {
            Timber.e(e, "Failed to setup Google Sign-In: %s", e.getMessage());
            throw new RuntimeException("Failed to setup Google Sign-In: " + e.getMessage());
        }
    }





}
