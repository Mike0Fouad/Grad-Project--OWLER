// app/ui/fragment/CalendarFragment.java
package com.owlerdev.owler.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.owlerdev.owler.R;
import com.owlerdev.owler.databinding.FragmentCalendarBinding;
import com.owlerdev.owler.utils.DateUtils;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CalendarFragment extends Fragment {
    private FragmentCalendarBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set current date
        String currentDate = DateUtils.getTodayDate();
        binding.tvCurrentDate.setText(currentDate);

        // Set up calendar listener
        binding.calendarView.setOnDateChangeListener(
                (view1, year, month, dayOfMonth) -> {
                    // month is 0-based, so add 1
                    String date = String.format(Locale.getDefault(),"%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    binding.tvCurrentDate.setText(date);
                    Bundle args = new Bundle();
                    args.putString("date", date);
                    NavHostFragment.findNavController(CalendarFragment.this)
                            .navigate(R.id.navigation_day, args);
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
