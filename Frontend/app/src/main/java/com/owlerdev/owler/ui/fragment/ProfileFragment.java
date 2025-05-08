package com.owlerdev.owler.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.owlerdev.owler.R;
import com.owlerdev.owler.databinding.FragmentProfileBinding;
import com.owlerdev.owler.model.calendar.GoogleFitData;
import com.owlerdev.owler.model.calendar.MLData;
import com.owlerdev.owler.ui.adapter.GoogleFitMetricAdapter;
import com.owlerdev.owler.viewmodel.ProfileViewModel;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private GoogleFitMetricAdapter googleFitAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupCharts();
//        setupGoogleFitRecycler();
        setupObservers();
    }

    private void setupCharts() {
        configureChart(binding.chartCp);
        configureChart(binding.chartPe);
    }

    private void configureChart(LineChart chart) {
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.getDescription().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(3600000f); // 1 hour in milliseconds
        xAxis.setTextColor(Color.DKGRAY);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.DKGRAY);
        leftAxis.setAxisMinimum(0f);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
    }

    private void setupGoogleFitRecycler() {
        googleFitAdapter = new GoogleFitMetricAdapter(new GoogleFitData());
        binding.recyclerGoogleFit.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerGoogleFit.setAdapter(googleFitAdapter);
    }

    private void setupObservers() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String dateStr = yesterday.format(DateTimeFormatter.ISO_DATE);

        // Observe ML data
        viewModel.getMlData(dateStr).observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess() && result.getData() != null && !result.getData().isEmpty()) {
                drawMLCharts(result.getData());
                binding.progressBar.setVisibility(View.GONE);
            } else {
                // Show mock data when real data is not available
                List<MLData> mockData = generateMockData();
                drawMLCharts(mockData);
                binding.progressBar.setVisibility(View.GONE);
            }
        });

//        // Observe Google Fit data
//        viewModel.getGoogleFitData(dateStr).observe(getViewLifecycleOwner(), result -> {
//            if (result.isSuccess() && result.getData() != null) {
//                googleFitAdapter.submitData(result.getData());
//                binding.progressBar.setVisibility(View.GONE);
//            } else {
//                binding.recyclerGoogleFit.setVisibility(View.GONE);
//            }
//        });

        // Observe user data
        viewModel.getCachedUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                Integer score = viewModel.getProductivityScore().getValue() != null ?
                        viewModel.getProductivityScore().getValue().getData() : null;
                if (score != null) {
                    binding.tvProductivityScore.setText(
                            String.format(Locale.getDefault(), "%d", score)
                    );
                } else {
                    // Show mock productivity score
                    binding.tvProductivityScore.setText("85 productivityScore");
                }
            }
        });
    }

    private void drawMLCharts(@NonNull List<MLData> data) {
        if (data.isEmpty()) {
            binding.chartCp.setNoDataText("No cognitive data");
            binding.chartPe.setNoDataText("No physical energy data");
            return;
        }

        processChartData(binding.chartCp, data,
                MLData::getPredicted_CP,
                ContextCompat.getColor(requireContext(), R.color.Cornflower_blue),
                "Cognitive Performance"
        );

        processChartData(binding.chartPe, data,
                MLData::getPredicted_PE,
                ContextCompat.getColor(requireContext(), R.color.Green_Blue),
                "Physical Energy"
        );

        binding.progressBar.setVisibility(View.GONE);
    }

    private void processChartData(LineChart chart, List<MLData> data,
                                  ValueExtractor<MLData> extractor,
                                  int color, String label) {
        List<Entry> entries = new ArrayList<>();
        float maxValue = 0f;

        for (int i = 0; i < data.size(); i++) {
            MLData item = data.get(i);
            float value = extractor.extract(item);
            long timestamp = Long.parseLong(item.getTime_slot());

            entries.add(new Entry(timestamp, value));
            if (value > maxValue) maxValue = value;
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawCircles(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(50);
        dataSet.setFillColor(color);

        chart.getAxisLeft().setAxisMaximum(maxValue * 1.1f);
        chart.setData(new LineData(dataSet));
        chart.getXAxis().setValueFormatter(new TimeAxisFormatter());
        chart.animateY(800, Easing.EaseInOutQuad);
        chart.invalidate();
    }

    private void handleChartError(String error) {
        binding.progressBar.setVisibility(View.GONE);
        Timber.e(error, "Chart data error");
        // Add error state UI handling here if needed
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private interface ValueExtractor<T> {
        float extract(T item);
    }

    private static class TimeAxisFormatter extends ValueFormatter {
        private final DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());

        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            return Instant.ofEpochMilli((long) value)
                    .atZone(ZoneId.systemDefault())
                    .format(formatter);
        }
    }

    private List<MLData> generateMockData() {
        List<MLData> mockData = new ArrayList<>();
        long startTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 hours ago
        long interval = 60 * 60 * 1000; // 1 hour intervals

        for (int i = 0; i < 24; i++) {
            MLData data = new MLData();
            data.setTime_slot(String.valueOf(startTime + (i * interval)));
            
            // Generate mock cognitive performance (CP) values between 60-90
            float cpValue = 60 + (float) (Math.random() * 30);
            data.setPredicted_CP(cpValue);
            
            // Generate mock physical energy (PE) values between 50-85
            float peValue = 50 + (float) (Math.random() * 35);
            data.setPredicted_PE(peValue);
            
            mockData.add(data);
        }
        return mockData;
    }
}
