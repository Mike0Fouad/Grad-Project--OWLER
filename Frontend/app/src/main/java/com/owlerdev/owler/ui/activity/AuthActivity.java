// app/ui/activity/AuthActivity.java
package com.owlerdev.owler.ui.activity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.owlerdev.owler.BuildConfig;
import com.owlerdev.owler.R;
import android.widget.RadioButton;
import com.owlerdev.owler.data.local.TokenManager;
import com.owlerdev.owler.databinding.ActivityAuthBinding;
import com.owlerdev.owler.viewmodel.AuthViewModel;
import com.owlerdev.owler.utils.Result;

import java.util.Objects;
import java.util.concurrent.Executors;
import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class AuthActivity extends AppCompatActivity {
    private ActivityAuthBinding binding;
    private AuthViewModel authViewModel;

    private CredentialManager credentialManager;
    private GetCredentialRequest googleRequest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        credentialManager = AuthViewModel.setupCredentialManager(this);
        googleRequest   = AuthViewModel.getGoogleRequest();
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        observeViewModel();
        setupUiListeners();
        showLoginForm();

    }



    private void observeViewModel() {
        authViewModel.getLoginResult().observe(this, this::handleAuthResult);
        authViewModel.getRegisterResult().observe(this, this::handleAuthResult);
        authViewModel.getGoogleCallbackResult().observe(this, this::handleAuthResult);
    }

    private void handleAuthResult(Result<?> result) {
        if (result.isSuccess()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            String err = result.getError() != null
                    ? result.getError()
                    : getString(R.string.error_generic);
            binding.authError.setText(err);
            binding.authError.setVisibility(View.VISIBLE);
        }
    }

    private void setupUiListeners() {
        binding.tvLoginTab.setOnClickListener(v -> showLoginForm());
        binding.tvRegisterTab.setOnClickListener(v -> showRegisterForm());

        binding.btnSubmit.setOnClickListener(v -> {
            binding.authError.setVisibility(View.GONE);

            String email = Objects.requireNonNull(binding.tilEmail.getEditText()).getText().toString().trim();
            String password = Objects.requireNonNull(binding.tilPassword.getEditText()).getText().toString().trim();

            if (binding.tvLoginTab.isSelected()) {
                authViewModel.login(email, password);
            } else {
                String name = Objects.requireNonNull(binding.tilName.getEditText()).getText().toString().trim();
                String ageText = Objects.requireNonNull(binding.tilAge.getEditText()).getText().toString().trim();

                if (ageText.isEmpty() || !ageText.matches("\\d+")) {
                    binding.tilAge.setError("Please enter a valid numeric age");
                    return;
                }

                int age = Integer.parseInt(ageText);

                int selectedId = binding.radioGroupGender.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    binding.authError.setText("Please select a gender");
                    binding.authError.setVisibility(View.VISIBLE);
                    return;
                }

                RadioButton selectedGender = findViewById(selectedId);
                String gender = selectedGender.getText().toString().trim().substring(0, 1);

                authViewModel.register(email, password, name, age, gender);
            }
        });

        binding.btnGoogleSignIn.setOnClickListener(v -> {
            binding.authError.setVisibility(View.GONE);
            promptGoogleSignIn();
        });
    }

    private void promptGoogleSignIn() {

        binding.authError.setVisibility(View.GONE);


        CancellationSignal cancellationSignal = new CancellationSignal();

        try {
            Timber.d("Starting Google Sign-In flow with client ID: %s", BuildConfig.CLIENT_ID);
            // 3. Kick off the federated sign-in flow
            credentialManager.getCredentialAsync(
                    this,
                    googleRequest,
                    cancellationSignal,
                    Executors.newSingleThreadExecutor(),
                    new CredentialManagerCallback<>() {
                        @Override
                        public void onResult(GetCredentialResponse response) {
                            Timber.d("Google Sign-In response received");

                            if (response.getCredential() instanceof GoogleIdTokenCredential) {
                                String idToken = ((GoogleIdTokenCredential) response.getCredential())
                                        .getIdToken();
                                Timber.d("Google ID token received successfully");

                                runOnUiThread(() -> authViewModel.googleSignin(idToken));
                            } else {

                                Timber.e("Unexpected credential type received: %s", 
                                    response.getCredential().getClass().getSimpleName());
                                runOnUiThread(() -> {
                                    binding.authError.setText(R.string.error_invalid_credential);
                                    binding.authError.setVisibility(View.VISIBLE);
                                });
                            }
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            String errorMessage;
                            String errorDetails = e.getMessage();
                            Timber.e("Google Sign-In Error: %s", errorDetails);

                            if (errorDetails != null) {
                                if (errorDetails.contains("No credentials available")) {
                                    errorMessage = "Please add a Google account to your device first";
                                } else if (errorDetails.contains("Cannot find a matching credential")) {
                                    errorMessage = "Please make sure you're signed in with a Google account";
                                } else if (errorDetails.contains("network")) {
                                    errorMessage = "Network error. Please check your internet connection";
                                } else if (errorDetails.contains("canceled")) {
                                    errorMessage = "Sign-in was canceled. Please try again.";
                                } else if (errorDetails.contains("DEVELOPER_ERROR")) {
                                    errorMessage = "Developer configuration error. Please contact support.";
                                } else if (errorDetails.contains("INVALID_CLIENT")) {
                                    errorMessage = "Invalid client configuration. Please contact support.";
                                } else if (errorDetails.contains("INVALID_ACCOUNT")) {
                                    errorMessage = "Invalid Google account. Please try with a different account.";
                                } else {
                                    errorMessage = "Google Sign-In failed: " + errorDetails;
                                }
                            } else {
                                errorMessage = "Google Sign-In failed. Please try again.";
                            }

                            runOnUiThread(() -> {
                                binding.authError.setText(errorMessage);
                                binding.authError.setVisibility(View.VISIBLE);
                                Timber.e("Google Sign-In failed with message: %s", errorMessage);
                            });
                        }
                    }
            );
        } catch (Exception e) {
            Timber.e(e, "Failed to start Google Sign-In: %s", e.getMessage());
            binding.authError.setText("Failed to start Google Sign-In. Please try again.");
            binding.authError.setVisibility(View.VISIBLE);
        }
    }

    private void showLoginForm() {
        binding.tvLoginTab.setSelected(true);
        binding.tvRegisterTab.setSelected(false);
        binding.tvLoginTab.setTypeface(null, android.graphics.Typeface.BOLD);
        binding.tvRegisterTab.setTypeface(null, Typeface.NORMAL);
        binding.tilName.setVisibility(View.GONE);
        binding.tilAge.setVisibility(View.GONE);
        binding.genderLayout.setVisibility(View.GONE);
        binding.btnSubmit.setText(R.string.action_login);
        binding.authError.setVisibility(View.GONE);
    }

    private void showRegisterForm() {
        binding.tvLoginTab.setSelected(false);
        binding.tvRegisterTab.setSelected(true);
        binding.tvRegisterTab.setTypeface(null, android.graphics.Typeface.BOLD);
        binding.tvLoginTab.setTypeface(null, Typeface.NORMAL);
        binding.tilName.setVisibility(View.VISIBLE);
        binding.tilAge.setVisibility(View.VISIBLE);
        binding.genderLayout.setVisibility(View.VISIBLE);
        binding.btnSubmit.setText(R.string.action_register);
        binding.authError.setVisibility(View.GONE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
