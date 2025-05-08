// app/model/GoogleFitData.java
package com.owlerdev.owler.model.calendar;

import java.util.Date;
import java.util.List;

/**
 * Represents a day's worth of Google Fit metrics for prediction and display.
 */
public class GoogleFitData {

    private List<HourlyMetric> hourly_metrics;
    private SleepStageData sleep;
    private float hrv;
    private Date last_updated;


    public GoogleFitData() { }

//    public GoogleFitData( List<HourlyMetric> hourly_metrics,
//                         SleepStageData sleep,
//                         float hrv,
//                         Date last_updated) {
//
//        this.hourly_metrics = hourly_metrics;
//        this.sleep         = sleep;
//        this.hrv           = hrv;
//        this.last_updated = last_updated;
//    }

    public List<HourlyMetric> getHourly_metrics() {
        return hourly_metrics;
    }
//    public void setHourly_metrics(List<HourlyMetric> hourly_metrics) {
//        this.hourly_metrics = hourly_metrics;
//    }

    public SleepStageData getSleep() {
        return sleep;
    }
//    public void setSleep(SleepStageData sleep) {
//        this.sleep = sleep;
//    }

    public float getHrv() {
        return hrv;
    }
//    public void setHrv(float hrv) {
//        this.hrv = hrv;
//    }
//    public Date getLast_updated() {
//        return last_updated;
//    }
//    public void setLast_updated(Date last_updated) {
//        this.last_updated = last_updated;
//    }

    /**
     * Encodes cyclical time features for ML.
     */
    public static class TimeFeatures {
        private float sin_time;
        private float cos_time;


//        public float getSin_time() {
//            return sin_time;
//        }
//        public void setSin_time(float sin_time) {
//            this.sin_time = sin_time;
//        }
//        public float getCos_time() {
//            return cos_time;
//        }
//        public void setCos_time(float cos_time) {
//            this.cos_time = cos_time;
//        }
    }

    /**
     * Metrics aggregated per hour slot.
     */
    public static class HourlyMetric {
        private String hour_range;      // e.g. "08:00-09:00"
        private int steps;
        private float heart_rate;
        private TimeFeatures time_features;


        public String getHour_range() {
            return hour_range;
        }
//        public void setHour_range(String hour_range) {
//            this.hour_range = hour_range;
//        }

        public int getSteps() {
            return steps;
        }
//        public void setSteps(int steps) {
//            this.steps = steps;
//        }

        public float getHeart_rate() {
            return heart_rate;
        }
//        public void setHeart_rate(float heart_rate) {
//            this.heart_rate = heart_rate;
//        }

        public TimeFeatures getTime_features() {
            return time_features;
        }
//        public void setTime_features(TimeFeatures time_features) {
//            this.time_features = time_features;
//        }
    }

    /**
     * Breakdown of sleep stages for the day.
     */
    public static class SleepStageData {
        private  float totalHours;
        private  float deepHours;
        private  float remHours;
        private  float lightHours;
        private  int awakeEpisodes;

        public SleepStageData(float totalHours, float deepHours, float remHours, float lightHours, int awakeEpisodes) {
            this.totalHours = totalHours;
            this.deepHours = deepHours;
            this.remHours = remHours;
            this.lightHours = lightHours;
            this.awakeEpisodes = awakeEpisodes;
        }
        public SleepStageData() {
        }

        public float getTotalHours() {
            return totalHours;
        }
//        public void setTotalHours(float totalHours) {
//            this.totalHours = totalHours;
//        }

        public float getDeepHours() {
            return deepHours;
        }
//        public void setDeepHours(float deepHours) {
//            this.deepHours = deepHours;
//        }

        public float getRemHours() {
            return remHours;
        }
//        public void setRemHours(float remHours) {
//            this.remHours = remHours;
//        }

        public float getLightHours() {
            return lightHours;
        }
//        public void setLightHours(float lightHours) {
//            this.lightHours = lightHours;
//        }

        public int getAwakeEpisodes() {
            return awakeEpisodes;
        }
//        public void setAwakeEpisodes(int awakeEpisodes) {
//            this.awakeEpisodes = awakeEpisodes;
//        }
    }

}
