package com.owlerdev.owler.ui.adapter;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TimePicker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.owlerdev.owler.R;

import java.util.List;

/**
 * DialogFragment for adding a new Task.
 */
public class AddTaskDialog extends DialogFragment {

    public void submitList(List<?> objects) {
    }

    public interface OnTaskActionListener {
        void onTaskAdded(String name, int startHour, int startMinute, int endHour, int endMinute,
                         int mentalLoad, int physicalLoad, int deadlineHour, int deadlineMinute);


    }

    private OnTaskActionListener listener;
    private EditText taskName;
    private TimePicker taskStart;
    private TimePicker taskEnd;
    private SeekBar sliderMental;
    private SeekBar sliderPhysical;
    private TimePicker taskDeadline;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (OnTaskActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTaskActionListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(getString(R.string.add_task));
        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        dialog.setContentView(view);

        // Initialize views
        taskName = view.findViewById(R.id.taskName);
        taskStart = view.findViewById(R.id.taskStart);
        taskEnd = view.findViewById(R.id.taskEnd);
        sliderMental = view.findViewById(R.id.sliderMental);
        sliderPhysical = view.findViewById(R.id.sliderPhysical);
        taskDeadline = view.findViewById(R.id.taskDeadline);
        ImageButton btnCancel = view.findViewById(R.id.close);
        Button btnSave = view.findViewById(R.id.btnSave);

        // Configure pickers
        taskStart.setIs24HourView(true);
        taskEnd.setIs24HourView(true);
        taskDeadline.setIs24HourView(true);

        btnCancel.setOnClickListener(v -> dismiss());

        btnSave.setOnClickListener(v -> {
            String name = taskName.getText().toString().trim();
            int startHour = taskStart.getHour();
            int startMinute = taskStart.getMinute();
            int endHour = taskEnd.getHour();
            int endMinute = taskEnd.getMinute();
            int mentalLoad = sliderMental.getProgress();
            int physicalLoad = sliderPhysical.getProgress();
            int deadlineHour = taskDeadline.getHour();
            int deadlineMinute = taskDeadline.getMinute();

            listener.onTaskAdded(name, startHour, startMinute, endHour, endMinute,
                    mentalLoad, physicalLoad, deadlineHour, deadlineMinute);

            dismiss();

        });

        return dialog;
    }
}
