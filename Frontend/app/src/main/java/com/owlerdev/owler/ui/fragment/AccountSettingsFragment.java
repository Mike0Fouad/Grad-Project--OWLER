package com.owlerdev.owler.ui.fragment;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.owlerdev.owler.R;
import com.owlerdev.owler.viewmodel.AuthViewModel;

import java.util.Objects;
import java.util.concurrent.Executors;
import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.owlerdev.owler.databinding.FragmentAccountSettingsBinding;
import com.owlerdev.owler.viewmodel.AccountSettingsViewModel;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

@AndroidEntryPoint
public class AccountSettingsFragment extends Fragment {
    private FragmentAccountSettingsBinding binding;
    private AccountSettingsViewModel settingsViewModel;
    private AuthViewModel authViewModel;

    @Inject CredentialManager credentialManager;
    private GetCredentialRequest googleRequest;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        settingsViewModel = new ViewModelProvider(this).get(AccountSettingsViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        credentialManager = AuthViewModel.setupCredentialManager(getContext());
        googleRequest   = AuthViewModel.getGoogleRequest();

        observeUser();
        setupListeners();
    }

    private void observeUser() {
        settingsViewModel.getCachedUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null && user.getData() != null) {
                Objects.requireNonNull(binding.tilName.getEditText()).setText(user.getData().getName());
                Objects.requireNonNull(binding.tilEmail.getEditText()).setText(user.getData().getEmail());
                if (user.getData().getAge() != null) {
                    Objects.requireNonNull(binding.tilAge.getEditText()).setText(String.valueOf(user.getData().getAge()));
                }
                if (user.getData().getGender() != null) {
                    if (user.getData().getGender().toString().equalsIgnoreCase("m")) {
                        binding.rbMale.setChecked(true);
                    } else if (user.getData().getGender().toString().equalsIgnoreCase("f")) {
                        binding.rbFemale.setChecked(true);
                    }
                }
            } else {

            }
        });
    }

    private void setupListeners() {
        binding.btnSaveProfile.setOnClickListener(v -> onSaveProfile());
        binding.btnChangePassword.setOnClickListener(v -> onChangePassword());
        binding.btnDeleteAccount.setOnClickListener(v -> onDeleteAccount());
        binding.btnLinkGoogle.setOnClickListener(v -> promptGoogleSignIn());
        binding.btnLogout.setOnClickListener(v -> logout());
    }

    private void promptGoogleSignIn() {
        // 1. Hide any previous error
        binding.authError.setVisibility(View.GONE);

        // 2. Prepare cancellation support
        CancellationSignal cancellationSignal = new CancellationSignal();

        // 3. Kick off the federated sign-in flow
        credentialManager.getCredentialAsync(
            requireContext(),
            googleRequest,
            cancellationSignal,
            Executors.newSingleThreadExecutor(),
            new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {

                        if (response.getCredential() instanceof GoogleIdTokenCredential) {
                            String idToken = ((GoogleIdTokenCredential) response.getCredential())
                                    .getIdToken();
                            // 4b. Hand it off to your AuthViewModel
                            requireActivity().runOnUiThread(() ->
                                    authViewModel.googleSignin(idToken)
                            );
                        } else {
                            // 4c. Unexpected credential type
                            requireActivity().runOnUiThread(() -> {
                                binding.authError.setText(R.string.error_invalid_credential);
                                binding.authError.setVisibility(View.VISIBLE);
                            });
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        // 5. Surface any errors to the user
                        requireActivity().runOnUiThread(() -> {
                            binding.authError.setText(e.getMessage());
                            binding.authError.setVisibility(View.VISIBLE);
                            Timber.e("Google Sign-In Error: %s", e.getMessage());
                        });
                    }
                }
        );
    }
    private void onSaveProfile() {
        String name = Objects.requireNonNull(binding.tilName.getEditText()).getText().toString().trim();
        String email = Objects.requireNonNull(binding.tilEmail.getEditText()).getText().toString().trim();
        String ageStr = Objects.requireNonNull(binding.tilAge.getEditText()).getText().toString().trim();
        RadioButton selectedGender =  binding.rgGender.findViewById(binding.rgGender.getCheckedRadioButtonId());
        if(selectedGender == null) return;

        String gender = selectedGender.getText().toString().trim().substring(0, 1);

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        if (!TextUtils.isEmpty(ageStr)) {
            updates.put("age", Integer.parseInt(ageStr));
        }
        updates.put("gender", gender);

        settingsViewModel.updateProfile(updates)
                .observe(getViewLifecycleOwner(), result -> {
                    if (result.isSuccess()) {
                        Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                    } else {
                        String err = result.getError() != null ? result.getError() : "Update failed";
                        Timber.e("Profile update error: %s", err);
                        Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void onChangePassword() {
        String oldPwd = Objects.requireNonNull(binding.tilOldPassword.getEditText()).getText().toString();
        String newPwd = Objects.requireNonNull(binding.tilNewPassword.getEditText()).getText().toString();
        if (TextUtils.isEmpty(oldPwd) || TextUtils.isEmpty(newPwd)) {
            Toast.makeText(getContext(), "Both passwords are required", Toast.LENGTH_SHORT).show();
            return;
        }
        settingsViewModel.changePassword(oldPwd, newPwd)
                .observe(getViewLifecycleOwner(), result -> {
                    if (result.isSuccess()) {
                        Toast.makeText(getContext(), "Password changed", Toast.LENGTH_SHORT).show();
                    } else {
                        String err = result.getError() != null ? result.getError() : "Change failed";
                        Timber.e("Password change error: %s", err);
                        Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void onDeleteAccount() {
        String Pwd = Objects.requireNonNull(binding.tilDeletePassword.getEditText()).getText().toString();

        settingsViewModel.deleteAccount(Pwd)
                .observe(getViewLifecycleOwner(), result -> {
                    if (result.isSuccess()) {
                        Toast.makeText(getContext(), "Account deleted", Toast.LENGTH_SHORT).show();
                        requireActivity().finish();
                    } else {
                        String err = result.getError() != null ? result.getError() : "Delete failed";
                        Timber.e("Account delete error: %s", err);
                        Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void logout() {
        settingsViewModel.logout()
                .observe(getViewLifecycleOwner(), result -> {
                    if (result.isSuccess()) {
                        Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
                        requireActivity().finish();
                    } else {
                        String err = result.getError() != null ? result.getError() : "Logout failed";
                        Timber.e("Logout error: %s", err);
                        Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}