package com.owlerdev.owler.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.owlerdev.owler.R;
import com.owlerdev.owler.model.calendar.Task;

import java.util.List;

public class TaskAdapter
        extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    public interface OnTaskActionListener {
        void onTaskChecked(Task task, boolean isChecked);
        //void onTaskEdit(Task task);
        void onTaskDelete(Task task);
        void onExhaustionChanged(Task task, int value);

    }

    private final List<Task> tasks;
    private final OnTaskActionListener listener;

    public TaskAdapter(List<Task> tasks, OnTaskActionListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    public void submitList(List<Task> tasks) {
        this.tasks.clear();
        this.tasks.addAll(tasks);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        h.bind(tasks.get(pos));
    }

    @Override public int getItemCount() {
        return tasks.size();
    }

    public List<Task> getCurrentList(){
        return tasks;}

    class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbDone;
        TextView tvName, tvStart, tvEnd, tvDeadline;
        TextView tvMental, tvPhysical;
        SeekBar sliderExhaustion;
        View exhaustionContainer;
        private boolean isUpdatingExhaustion = false;

        ViewHolder(View itemView) {
            super(itemView);
            cbDone     = itemView.findViewById(R.id.cbDone);
            tvName     = itemView.findViewById(R.id.tvTaskName);
            tvStart    = itemView.findViewById(R.id.tvStartTime);
            tvEnd      = itemView.findViewById(R.id.tvEndTime);
            tvDeadline = itemView.findViewById(R.id.tvDeadline);
            tvMental   = itemView.findViewById(R.id.tvMental);
            tvPhysical = itemView.findViewById(R.id.tvPhysical);
            exhaustionContainer = itemView.findViewById(R.id.exhaustionContainer);
            sliderExhaustion    = itemView.findViewById(R.id.sliderExhaustion);

//            itemView.findViewById(R.id.btnEdit)
//                    .setOnClickListener(v ->
//                            listener.onTaskEdit(tasks.get(getBindingAdapterPosition()))
//                    );
            itemView.findViewById(R.id.btnDelete)
                    .setOnClickListener(v ->
                            listener.onTaskDelete(tasks.get(getBindingAdapterPosition()))
                    );
        }

        void bind(Task t) {
            tvName.setText(t.getName());
            
            // Set up checkbox listener
            cbDone.setOnCheckedChangeListener(null);
            cbDone.setChecked(t.isDone());
            cbDone.setOnCheckedChangeListener((btn, isChecked) -> {
                listener.onTaskChecked(t, isChecked);
                // Update exhaustion bar visibility immediately
                exhaustionContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                if (isChecked) {
                    sliderExhaustion.setProgress(t.getExhaustion());
                }
            });

            tvStart.setText(formatTimeAmPm(t.getStart()));
            tvEnd.setText(formatTimeAmPm(t.getEnd()));
            tvDeadline.setText(formatTimeAmPm(t.getDeadline()));
            tvMental.setText(String.valueOf(t.getMental()));
            tvPhysical.setText(String.valueOf(t.getPhysical()));

            // Set initial exhaustion bar visibility
            exhaustionContainer.setVisibility(t.isDone() ? View.VISIBLE : View.GONE);
            
            // Set up exhaustion slider
            isUpdatingExhaustion = true;
            sliderExhaustion.setProgress(t.getExhaustion());
            isUpdatingExhaustion = false;
            
            sliderExhaustion.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                        @Override 
                        public void onProgressChanged(SeekBar sb, int value, boolean fromUser) {
                            if (!isUpdatingExhaustion && fromUser) {
                                listener.onExhaustionChanged(t, value);
                            }
                        }
                        @Override public void onStartTrackingTouch(SeekBar sb) {}
                        @Override public void onStopTrackingTouch(SeekBar sb) {}
                    }
            );
        }

        private String formatTimeAmPm(String time24) {
            if (time24 == null || time24.isEmpty()) return "";
            try {
                java.text.SimpleDateFormat sdf24 = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                java.text.SimpleDateFormat sdf12 = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
                java.util.Date date = sdf24.parse(time24);
                return sdf12.format(date);
            } catch (Exception e) {
                return time24;
            }
        }
    }
}
