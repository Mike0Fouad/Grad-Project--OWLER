package com.owlerdev.owler.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.owlerdev.owler.R;
import com.owlerdev.owler.databinding.FragmentDayBinding;
import com.owlerdev.owler.model.calendar.Task;
import com.owlerdev.owler.model.calendar.Schedule;
import com.owlerdev.owler.ui.activity.AuthActivity;
import com.owlerdev.owler.ui.adapter.AddTaskDialog;
import com.owlerdev.owler.ui.adapter.TaskAdapter;
import com.owlerdev.owler.viewmodel.DayViewModel;
import com.owlerdev.owler.viewmodel.NotificationViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DayFragment extends Fragment implements TaskAdapter.OnTaskActionListener,
        AddTaskDialog.OnTaskActionListener  {

    private FragmentDayBinding binding;
    private DayViewModel viewModel;
    private NotificationViewModel notificationViewModel;
    private String date;
    private AddTaskDialog addTaskDialog;
    private TaskAdapter taskAdapter;
    private final int exhaustionLevel=5;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DayViewModel.class);
        notificationViewModel = new ViewModelProvider(this).get(NotificationViewModel.class);
        if (getArguments() != null) {
            date = getArguments().getString("date");
        }
        if (date == null || date.isEmpty()) {
            date = com.owlerdev.owler.utils.DateUtils.getTodayDate();
        }
        addTaskDialog = new AddTaskDialog();
        setupRecyclerView();
//      setupExhaustionSlider();
        setupButtons();
        observeSchedule();
        setupFragmentResultListener();
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(new ArrayList<>(), this);

        binding.recyclerTasks.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );
        binding.recyclerTasks.setAdapter(taskAdapter);

        viewModel.getSchedule(date).observe(getViewLifecycleOwner(), schedule -> {
            if (schedule != null && schedule.getData() != null) {
            schedule.getData().arrangeTasks();
            List<Task> tasks = schedule.getData().getTasks();
            taskAdapter.submitList(tasks);
                taskAdapter.notifyDataSetChanged();
            } else {
                taskAdapter.submitList(new ArrayList<>());
                taskAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setupButtons() {
        binding.btnAddTask.setOnClickListener(v -> {
            showAddTaskDialog();
        });



        binding.btnSuggestSchedule.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            viewModel.suggestSchedule(date).observe(getViewLifecycleOwner(), result -> {
                binding.progressBar.setVisibility(View.GONE);
                if (result.isSuccess() && result.getData() != null) {
                    Schedule schedule = result.getData();
                    List<String> suggestions = schedule.getTasks()
                            .stream()
                            .map(Task::getName)
                            .filter(name -> name != null && !name.isEmpty())
                            .collect(Collectors.toList());
                    if (!suggestions.isEmpty()) {
                        navigateToSuggestions(new ArrayList<>(suggestions));
                    } else {
                        showToast(R.string.no_suggestions_available);
                    }
                } else {
                    showToast(R.string.no_suggestions_available);
                }
            });
        });
    }

    private void showAddTaskDialog() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);

        EditText taskNameInput = dialogView.findViewById(R.id.taskName);
        TimePicker startPicker = dialogView.findViewById(R.id.taskStart);
        TimePicker endPicker = dialogView.findViewById(R.id.taskEnd);
        SeekBar sliderMental = dialogView.findViewById(R.id.sliderMental);
        SeekBar sliderPhysical = dialogView.findViewById(R.id.sliderPhysical);
        TimePicker deadlinePicker = dialogView.findViewById(R.id.taskDeadline);
        TextView tvMentalValue = dialogView.findViewById(R.id.tvMentalValue);
        TextView tvPhysicalValue = dialogView.findViewById(R.id.tvPhysicalValue);

        // Set time pickers to 12-hour format
        startPicker.setIs24HourView(false);
        endPicker.setIs24HourView(false);
        deadlinePicker.setIs24HourView(false);

        // Set initial values
        tvMentalValue.setText(String.valueOf(sliderMental.getProgress()));
        tvPhysicalValue.setText(String.valueOf(sliderPhysical.getProgress()));

        // Update values when sliders change
        sliderMental.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvMentalValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sliderPhysical.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvPhysicalValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.new_task)
                .setView(dialogView)
                .setPositiveButton(R.string.add, null)
                .setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = dialogBuilder.show();

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (validateTaskInput(taskNameInput,startPicker, endPicker)) {
                String taskName = taskNameInput.getText().toString().trim();

                String startTime = String.format(Locale.getDefault(), "%02d:%02d", startPicker.getHour(), startPicker.getMinute());
                String endTime = String.format(Locale.getDefault(), "%02d:%02d", endPicker.getHour(), endPicker.getMinute());
                String deadlineTime = String.format(Locale.getDefault(), "%02d:%02d", deadlinePicker.getHour(), deadlinePicker.getMinute());

                int mental = sliderMental.getProgress();
                int physical = sliderPhysical.getProgress();

                Task newTask = new Task(taskName);
                newTask.setStart(startTime);
                newTask.setEnd(endTime);
                newTask.setDeadline(deadlineTime);
                newTask.setMental(mental);
                newTask.setPhysical(physical);

                Boolean canSchedule = notificationViewModel.canScheduleExactAlarms().getValue() != null ? 
                    notificationViewModel.canScheduleExactAlarms().getValue().getData() : false;
                if (Boolean.TRUE.equals(canSchedule)) {
                notificationViewModel.scheduleReminder(newTask);
                }

                // Add task and observe the result
                viewModel.addTask(date, newTask).observe(getViewLifecycleOwner(), result -> {
                    if (result.isSuccess()) {
                        // Refresh the schedule to show the new task
                        viewModel.getSchedule(date).observe(getViewLifecycleOwner(), scheduleResult -> {
                            if (scheduleResult.isSuccess() && scheduleResult.getData() != null) {
                                scheduleResult.getData().arrangeTasks();
                                List<Task> tasks = scheduleResult.getData().getTasks();
                                taskAdapter.submitList(tasks);
                                taskAdapter.notifyDataSetChanged();
                            }
                        });
                    } else {
                        showToast(R.string.error_adding_task);
                    }
                });

                dialog.dismiss();
            }
        });
    }

    private boolean validateTaskInput(EditText taskNameInput, TimePicker startPicker, TimePicker endPicker) {
        String taskName = taskNameInput.getText().toString().trim();

        if (taskName.isEmpty()) {
            taskNameInput.setError(getString(R.string.enter_task_name));
            taskNameInput.requestFocus();
            return false;
        }

        int startHour = startPicker.getHour();
        int startMinute = startPicker.getMinute();
        int endHour = endPicker.getHour();
        int endMinute = endPicker.getMinute();

        // Convert times to minutes since midnight for comparison
        int startTotalMinutes = startHour * 60 + startMinute;
        int endTotalMinutes = endHour * 60 + endMinute;

        if (endTotalMinutes <= startTotalMinutes) {
            Toast.makeText(requireContext(), getString(R.string.invalid_time_range), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

//    private void showEditTaskDialog(Task task) {
//        LayoutInflater inflater = LayoutInflater.from(requireContext());
//        View dialogView = inflater.inflate(R.layout.dialog_add_task, null); // reuse same layout
//
//        EditText taskNameInput = dialogView.findViewById(R.id.taskName);
//        TimePicker startPicker = dialogView.findViewById(R.id.taskStart);
//        TimePicker endPicker = dialogView.findViewById(R.id.taskEnd);
//        SeekBar sliderMental = dialogView.findViewById(R.id.sliderMental);
//        SeekBar sliderPhysical = dialogView.findViewById(R.id.sliderPhysical);
//        TimePicker deadlinePicker = dialogView.findViewById(R.id.taskDeadline);
//
//        // Prefill existing task data
//        taskNameInput.setText(task.getName());
//
//        String[] startParts = task.getStart().split(":");
//        startPicker.setHour(Integer.parseInt(startParts[0]));
//        startPicker.setMinute(Integer.parseInt(startParts[1]));
//
//        String[] endParts = task.getEnd().split(":");
//        endPicker.setHour(Integer.parseInt(endParts[0]));
//        endPicker.setMinute(Integer.parseInt(endParts[1]));
//
//        String[] deadlineParts = task.getDeadline().split(":");
//        deadlinePicker.setHour(Integer.parseInt(deadlineParts[0]));
//        deadlinePicker.setMinute(Integer.parseInt(deadlineParts[1]));
//
//        sliderMental.setProgress(task.getMental());
//        sliderPhysical.setProgress(task.getPhysical());
//
//        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireContext())
//                .setTitle(R.string.edit_task)
//                .setView(dialogView)
//                .setPositiveButton(R.string.save, null)
//                .setNegativeButton(R.string.cancel, null);
//
//        AlertDialog dialog = dialogBuilder.show();
//
//        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
//            if (validateTaskInput(taskNameInput, startPicker, endPicker)) {
//                String taskName = taskNameInput.getText().toString().trim();
//                String startTime = String.format(Locale.getDefault(), "%02d:%02d", startPicker.getHour(), startPicker.getMinute());
//                String endTime = String.format(Locale.getDefault(), "%02d:%02d", endPicker.getHour(), endPicker.getMinute());
//                String deadline = String.format(Locale.getDefault(), "%02d:%02d", deadlinePicker.getHour(), deadlinePicker.getMinute());
//
//                int mental = sliderMental.getProgress();
//                int physical = sliderPhysical.getProgress();
//
//                // Update task object
//                task.setName(taskName);
//                task.setStart(startTime);
//                task.setEnd(endTime);
//                task.setDeadline(deadline);
//                task.setMental(mental);
//                task.setPhysical(physical);
//
//                // Call ViewModel update methods
//                viewModel.renameTask(date, task, taskName);
//                Task reminderTaskName;
//                reminderTaskName = task;
//                reminderTaskName.setName(taskName);
//
//
//
//                notificationViewModel.rescheduleReminder(task);
//                viewModel.updateTaskMental(date, task, mental);
//                viewModel.updateTaskPhysical(date, task, physical);
//                viewModel.updateTaskStartTime(date, task, startTime);
//
//                Task reminderTaskStart;
//                reminderTaskStart = task;
//                reminderTaskStart.setStart(startTime);
//                notificationViewModel.rescheduleReminder(reminderTaskStart);
//                viewModel.updateTaskEndTime(date, task, endTime);
//                viewModel.updateTaskDeadline(date, task, deadline);
//
//                dialog.dismiss();
//            }
//        });
//    }


    private void navigateToSuggestions(ArrayList<String> suggestions) {
        Bundle args = new Bundle();
        args.putStringArrayList("suggestions", suggestions);

        NavController navController = NavHostFragment.findNavController(this);
        // navController.navigate(R.id.dialog_suggest_schedule, args);
    }
    private void showSuggestDialog(Schedule suggested) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_suggest_schedule, null);
        RecyclerView rv = dialogView.findViewById(R.id.recycler_tasks);
        TaskAdapter suggestAdapter = new TaskAdapter(
                new ArrayList<>(suggested.getTasks()),
                new TaskAdapter.OnTaskActionListener() {
                    @Override public void onTaskChecked(Task t, boolean c) {}
                    //@Override public void onTaskEdit(Task t) {}
                    @Override public void onTaskDelete(Task t) {}
                    @Override public void onExhaustionChanged(Task t, int v) {}

                });
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(suggestAdapter);


        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();


        dialogView.findViewById(R.id.btnDiscard)
                .setOnClickListener(v -> dialog.dismiss());


        dialogView.findViewById(R.id.btnSaveSuggestion)
                .setOnClickListener(v -> {
                    // Persist your new schedule:
                    viewModel.changeSchedule(date, suggested);
                    dialog.dismiss();
                });

        dialog.show();
    }


    private void observeSchedule() {
        viewModel.getSchedule(date).observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess()) {
                Schedule schedule = result.getData();
                addTaskDialog.submitList(schedule != null ? schedule.getTasks() : new ArrayList<>());
            } else {
                String error = result.getError();
                if (error != null && error.contains("Unauthorized")) {

                    Intent intent = new Intent(requireContext(), AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
            } else {
                showToast(R.string.error_loading_schedule);
                }
            }
        });
    }

    private void setupFragmentResultListener() {
        getParentFragmentManager().setFragmentResultListener(
                "suggestion_selected",
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    if (requestKey.equals("suggestion_selected")) {
                        String date = result.getString("selected_schedule");
                        viewModel.suggestSchedule(date);
                    }
                }
        );
    }

    private void showToast(int stringResId) {
        Toast.makeText(requireContext(), stringResId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onTaskAdded(String name, int startHour, int startMinute, int endHour, int endMinute, int mentalLoad, int physicalLoad, int deadlineHour, int deadlineMinute) {
        String startTime = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute);
        String endTime = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute);
        String deadlineTime = String.format(Locale.getDefault(), "%02d:%02d", deadlineHour, deadlineMinute);

        Task newTask = new Task(name);
        newTask.setStart(startTime);
        newTask.setEnd(endTime);
        newTask.setDeadline(deadlineTime);
        newTask.setMental(mentalLoad);
        newTask.setPhysical(physicalLoad);

        viewModel.addTask(date, newTask);
        if (Boolean.TRUE.equals(Objects.requireNonNull(notificationViewModel.canScheduleExactAlarms().getValue()).getData())) {
            notificationViewModel.scheduleReminder(newTask);
        }

    }



    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        if (isChecked) {
            viewModel.checkTask(date, task);
        } else {
            viewModel.uncheckTask(date, task);
        }

    }

//    @Override
//    public void onTaskEdit(Task task) {
//        showEditTaskDialog(task);
//
//    }

    @Override
    public void onTaskDelete(Task task) {
        viewModel.deleteTask(date,task);
        notificationViewModel.cancelReminder(task);

        List<Task> current = new ArrayList<>(taskAdapter.getCurrentList());
        int pos = current.indexOf(task);
        if (pos != -1) {
            current.remove(pos);
            taskAdapter.submitList(current);
            taskAdapter.notifyItemRemoved(pos);
        }

    }





    @Override
    public void onExhaustionChanged(Task task, int value) {
        task.setExhaustion(value);
        viewModel.updateTaskExhaustion(date, task, value); // Persist the change
        
        // Update the task in the current list without notifying the adapter
        List<Task> current = taskAdapter.getCurrentList();
        int pos = current.indexOf(task);
        if (pos != -1) {
            current.set(pos, task);
        }
    }




}