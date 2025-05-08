// app/model/MLData.java
package com.owlerdev.owler.model.calendar;

public class MLData {
    /**
     * Time slot for the prediction, e.g. "08:00-09:00"
     */
    private String time_slot;

    /**
     * Predicted cognitive performance (0.0–1.0)
     */
    private float predicted_CP;

    /**
     * Predicted physical energy (0.0–1.0)
     */
    private float predicted_PE;

    public MLData() { }

    public MLData(String time_slot, float predicted_CP, float predicted_PE) {
        this.time_slot = time_slot;
        this.predicted_CP = predicted_CP;
        this.predicted_PE = predicted_PE;
    }

    public String getTime_slot() {
        return time_slot;
    }
    public void setTime_slot(String time_slot) {
        this.time_slot = time_slot;
    }

    public float getPredicted_CP() {
        return predicted_CP;
    }
    public void setPredicted_CP(float predicted_CP) {
        this.predicted_CP = predicted_CP;
    }

    public float getPredicted_PE() {
        return predicted_PE;
    }
    public void setPredicted_PE(float predicted_PE) {
        this.predicted_PE = predicted_PE;
    }
}
