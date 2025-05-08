// app/ui/viewmodel/ProfileViewModel.java
package com.owlerdev.owler.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;

import com.owlerdev.owler.model.calendar.GoogleFitData;
import com.owlerdev.owler.model.calendar.MLData;
import com.owlerdev.owler.model.user.User;
import com.owlerdev.owler.service.CalendarService;
import com.owlerdev.owler.service.UserService;
import com.owlerdev.owler.utils.Result;

import java.util.List;

import javax.inject.Inject;

@HiltViewModel
public class ProfileViewModel extends ViewModel {
    private final UserService userService;
    private final CalendarService calendarService;

    @Inject
    public ProfileViewModel(UserService userService, CalendarService calendarService) {
        this.userService = userService;
        this.calendarService = calendarService;
    }

    public LiveData<Result<User>> getCachedUser() {
         return userService.getCachedUser();
    }

    public LiveData<Result<List<MLData>>> getMlData(String date) {
        return calendarService.getMlData(date);
    }

    public LiveData<Result<GoogleFitData>> getGoogleFitData(String date) {
        return calendarService.getGoogleFitData(date);
    }

    public LiveData<Result<Integer>> getProductivityScore() {
        return userService.getProductivityScore();
    }



}