// app/data/repository/CalendarRepository.java
package com.owlerdev.owler.data.repository;

import static com.owlerdev.owler.utils.JsonUtils.gson;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.owlerdev.owler.data.local.JsonStorageManager;
import com.owlerdev.owler.data.remote.ApiService;
import com.owlerdev.owler.model.calendar.Day;
import com.owlerdev.owler.model.calendar.MLData;
import com.owlerdev.owler.model.calendar.GoogleFitData;
import com.owlerdev.owler.model.calendar.Schedule;
import com.owlerdev.owler.model.calendar.Task;
import com.owlerdev.owler.model.calendar.UserData;
import com.owlerdev.owler.network.NetworkMonitor;
import com.owlerdev.owler.utils.Result;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

@Singleton
public class CalendarRepository {
    private final ApiService authApiService;
    private final JsonStorageManager storage;
    private final NetworkMonitor networkMonitor;

    @Inject
    public CalendarRepository(@Named("authApiService") ApiService authApiService,
                              JsonStorageManager storage,
                              NetworkMonitor networkMonitor) {
        this.authApiService = authApiService;
        Timber.d("CalendarRepository: authApiService class = %s", authApiService.getClass().getName());
        this.storage = storage;
        this.networkMonitor = networkMonitor;

    }

    public LiveData<Result<Schedule>> getSchedule(String date) {
        Timber.d("CalendarRepository: authApiService class = %s", authApiService.getClass().getName());
        MutableLiveData<Result<Schedule>> live = new MutableLiveData<>();
        Schedule local = storage.getSchedule(date);

        // If no local schedule, create and save a new one
        if (local == null) {
            local = new Schedule();
            storage.addOrUpdateSchedule(date, local);
        }
        final Schedule finalLocal = local;
        live.postValue(Result.success(finalLocal));

        observeOnline(() -> authApiService.getDays(date)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> res) {
                        if (res.isSuccessful() && res.body() != null) {
                            try {
                                Day day = gson.fromJson(res.body(), Day.class);
                                Schedule sched = day.getSchedule();
                                storage.addOrUpdateSchedule(date, sched);
                                live.postValue(Result.success(sched));
                            } catch (Exception e) {
                                Timber.e(e);
                                live.postValue(Result.error(Objects.requireNonNull(e.getMessage())));
                            }
                        } else if (res.code() == 401) {

                            live.postValue(Result.success(finalLocal));
                        } else {
                            live.postValue(Result.error("Error " + res.code()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        Timber.e(t);

                        live.postValue(Result.success(finalLocal));
                    }
                }));
        return live;
    }

    public LiveData<Result<Void>> addSchedule(String date, Schedule schedule) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        storage.addOrUpdateSchedule(date, schedule);
        live.postValue(Result.success(null));
        observeOnline(() -> {
            JsonObject payload = new JsonObject();
            Day day = dayFromSchedule(date, schedule);
            Map<String, Object> days = new HashMap<>();
            days.put("date", day.getDate());
            days.put("schedule", day.getSchedule());
//            days.put("userData", day.getUserData());
            payload.add("days", gson.toJsonTree(days));
            authApiService.uploadDays(payload)
                    .enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> res) {
                            if (res.isSuccessful()) live.postValue(Result.success(null));
                            else live.postValue(Result.error("Error " + res.code()));
                        }

                        @Override
                        public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                            Timber.e(t);
                            live.postValue(Result.error(Objects.requireNonNull(t.getMessage())));
                        }
                    });
        });
        return live;
    }

    public LiveData<Result<Void>> addTask( String date, Task task) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        Day day = storage.loadDay(date);
        if (day == null) day = new Day(date, null, null);
        day.addOrUpdateTask(task);
        storage.addOrUpdateDay(day);
        live.postValue(Result.success(null));
        return addSchedule( date, day.getSchedule());
    }

    public LiveData<Result<Void>> removeTask(String date, Task task) {
        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
        try {
            Day day = storage.loadDay(date);
            if (day == null) {
                day = new Day(date, new Schedule(), null);
            }
            day.removeTask(task);
            storage.addOrUpdateDay(day);
            return addSchedule(date, day.getSchedule());
        } catch (Exception e) {
            Timber.e(e, "Error removing task");
            live.postValue(Result.error("Failed to remove task: " + e.getMessage()));
            return live;
        }
    }

    public LiveData<Result<Void>> editTask( String date, Task task) {

        return addTask(date, task);
    }

    public LiveData<Result<UserData>> getUserData( String date) {
        MutableLiveData<Result<UserData>> live = new MutableLiveData<>();
        UserData local = storage.getUserData(date);
        if (local != null) live.postValue(Result.success(local));

        observeOnline(() -> authApiService.getUserData(date)
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> res) {
                        if (res.isSuccessful() && res.body() != null) {
                            UserData ud = gson.fromJson(res.body(), UserData.class);
                            storage.addOrUpdateUserData(date, ud);
                            live.postValue(Result.success(ud));
                        } else live.postValue(Result.error("Error " + res.code()));
                    }

                    @Override
                    public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                        Timber.e(t);
                        live.postValue(Result.error(Objects.requireNonNull(t.getMessage())));
                    }
                }));
        return live;
    }


    public LiveData<Result<List<MLData>>> getMlData(String date) {
        MutableLiveData<Result<List<MLData>>> live = new MutableLiveData<>();
        List<MLData> local = storage.getMlData(date);
        if (local != null) live.postValue(Result.success(local));

        getUserData( date).observeForever(result -> {
            if (result.isSuccess()) {
                assert result.getData() != null;
                live.postValue(Result.success(result.getData().getMlData()));
            }
        });
        return live;
    }

    public LiveData<Result<GoogleFitData>> getGoogleFitData( String date) {
        MutableLiveData<Result<GoogleFitData>> live = new MutableLiveData<>();
        GoogleFitData local = storage.getGoogleFitData(date);
        if (local != null) live.postValue(Result.success(local));

        getUserData( date).observeForever(result -> {
            if (result.isSuccess()) {
                assert result.getData() != null;
                live.postValue(Result.success(result.getData().getGoogleFitData()));
            }
        });
        return live;
    }

//    public MutableLiveData<Result<Void>> saveLocalDay(Day day) {
//        storage.addOrUpdateDay(day);
//        MutableLiveData<Result<Void>> live = new MutableLiveData<>();
//        live.postValue(Result.success(null));
//        return live;
//    }

    // ------------------------------------
    // Helpers
    // ------------------------------------

    private void observeOnline(Runnable action) {
        Observer<Boolean> obs = new Observer<>() {
            @Override
            public void onChanged(Boolean connected) {
                if (connected) {
                    action.run();
                    networkMonitor.getIsConnected().removeObserver(this);
                }
            }
        };
        networkMonitor.getIsConnected().observeForever(obs);

    }

    private Day dayFromSchedule(String date, Schedule schedule) {
        Day d = storage.loadDay(date);
        return new Day(date, schedule, d != null ? d.getUserData() : null);
    }
}