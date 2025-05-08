// app/model/UserData.java
package com.owlerdev.owler.model.calendar;

import java.util.List;

public class UserData {
    /** Raw Google Fit metrics for the day */
    private GoogleFitData googleFitData;

    /** Summary schedule metrics and task list */
    private Schedule scheduleData;

    /** Aggregated task statistics per time slot */
    private AggregatedTaskData aggregatedTaskData;

    /** Hourly ML predictions of CP & PE */
    private List<MLData> mlData;

    public UserData() { }

    public UserData(GoogleFitData googleFitData,
                    Schedule scheduleData,
                    AggregatedTaskData aggregatedTaskData,
                    List<MLData> mlData) {
        this.googleFitData       = googleFitData;
        this.scheduleData        = scheduleData;
        this.aggregatedTaskData  = aggregatedTaskData;
        this.mlData              = mlData;
    }

    public GoogleFitData getGoogleFitData() {
        return googleFitData;
    }
    public void setGoogleFitData(GoogleFitData googleFitData) {
        this.googleFitData = googleFitData;
    }

    public Schedule getScheduleData() {
        return scheduleData;
    }
    public void setScheduleData(Schedule scheduleData) {
        this.scheduleData = scheduleData;
    }

    public AggregatedTaskData getAggregatedTaskData() {
        return aggregatedTaskData;
    }
    public void setAggregatedTaskData(AggregatedTaskData aggregatedTaskData) {
        this.aggregatedTaskData = aggregatedTaskData;
    }

    public List<MLData> getMlData() {
        return mlData;
    }
    public void setMlData(List<MLData> mlData) {
        this.mlData = mlData;
    }


}
