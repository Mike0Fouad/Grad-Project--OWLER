// app/ui/adapter/GoogleFitMetricAdapter.java
package com.owlerdev.owler.ui.adapter;

import static java.util.Locale.getDefault;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.owlerdev.owler.databinding.ItemGoogleFitMetricBinding;
import com.owlerdev.owler.model.calendar.GoogleFitData;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to display key Google Fit metrics in a RecyclerView.
 */
public class GoogleFitMetricAdapter extends RecyclerView.Adapter<GoogleFitMetricAdapter.MetricVH> {
    private final List<Metric> metrics;

    public GoogleFitMetricAdapter(GoogleFitData data) {
        metrics = new ArrayList<>();
        // Aggregate hourly metrics
        if (data.getHourly_metrics() != null && !data.getHourly_metrics().isEmpty()) {
            int totalSteps = 0;
            double totalHeart = 0;
            for (var m : data.getHourly_metrics()) {
                totalSteps += m.getSteps();
                totalHeart += m.getHeart_rate();
            }
            int count = data.getHourly_metrics().size();
            metrics.add(new Metric("Total Steps", String.valueOf(totalSteps)));
            metrics.add(new Metric("Avg Heart Rate", count>0 ? String.format(getDefault(),"%.1f", totalHeart/count) : "0"));
        }
        // Sleep stages
        if (data.getSleep() != null) {
            metrics.add(new Metric("Total Sleep (h)", String.valueOf(data.getSleep().getTotalHours())));
            metrics.add(new Metric("Deep Sleep (h)", String.valueOf(data.getSleep().getDeepHours())));
            metrics.add(new Metric("REM Sleep (h)", String.valueOf(data.getSleep().getRemHours())));
            metrics.add(new Metric("Light Sleep (h)", String.valueOf(data.getSleep().getLightHours())));
            metrics.add(new Metric("Awake Episodes", String.valueOf(data.getSleep().getAwakeEpisodes())));
        }
        // HRV
        metrics.add(new Metric("HRV", String.valueOf(data.getHrv())));
    }
    public void submitData(GoogleFitData data) {

        metrics.clear();


        if (data.getHourly_metrics() != null && !data.getHourly_metrics().isEmpty()) {
            int totalSteps = 0;
            double totalHeart = 0;
            for (var m : data.getHourly_metrics()) {
                totalSteps += m.getSteps();
                totalHeart += m.getHeart_rate();
            }
            int count = data.getHourly_metrics().size();
            metrics.add(new Metric("Total Steps", String.valueOf(totalSteps)));
            metrics.add(new Metric("Avg Heart Rate", count > 0 ?
                    String.format(getDefault(),"%.1f", totalHeart/count) : "0"));
        }

        if (data.getSleep() != null) {
            metrics.add(new Metric("Total Sleep (h)", String.valueOf(data.getSleep().getTotalHours())));
            metrics.add(new Metric("Deep Sleep (h)", String.valueOf(data.getSleep().getDeepHours())));
            metrics.add(new Metric("REM Sleep (h)", String.valueOf(data.getSleep().getRemHours())));
            metrics.add(new Metric("Light Sleep (h)", String.valueOf(data.getSleep().getLightHours())));
            metrics.add(new Metric("Awake Episodes", String.valueOf(data.getSleep().getAwakeEpisodes())));
        }

        if (data.getHrv() != 0) {
            metrics.add(new Metric("HRV", String.valueOf(data.getHrv())));
        }
        notifyItemRangeInserted(0, metrics.size());
    }
    @NonNull
    @Override
    public MetricVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGoogleFitMetricBinding binding = ItemGoogleFitMetricBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MetricVH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MetricVH holder, int position) {
        Metric m = metrics.get(position);
        holder.binding.tvMetricName.setText(m.name);
        holder.binding.tvMetricValue.setText(m.value);
    }

    @Override
    public int getItemCount() {
        return metrics.size();
    }

    public static class MetricVH extends RecyclerView.ViewHolder {
        final ItemGoogleFitMetricBinding binding;
        MetricVH(ItemGoogleFitMetricBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static class Metric {
        final String name;
        final String value;

        Metric(String name, String value) {
            this.name = name;
            this.value = value;

        }

    }
}
