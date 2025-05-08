// app/model/AggregatedTaskData.java
package com.owlerdev.owler.model.calendar;

import java.util.Map;

/**
 * Holds aggregated task metrics per time slot for a given day.
 */
public class AggregatedTaskData {
    /**
     * Mapping from time slot key (e.g., "08:00-09:00") to slot metrics.
     */
    private Map<String, SlotData> slots;

    public AggregatedTaskData() { }

    public AggregatedTaskData(Map<String, SlotData> slots) {
        this.slots = slots;
    }

    public Map<String, SlotData> getSlots() {
        return slots;
    }

    public void setSlots(Map<String, SlotData> slots) {
        this.slots = slots;
    }

    /**
     * Represents aggregated metrics for a single time slot.
     */
    public static class SlotData {
        private float totalMental;
        private float totalPhysical;
        private float totalExhaustion;
        private int totalDuration;    // in minutes
        private float avgMental;
        private float avgPhysical;
        private float avgExhaustion;

        public SlotData() { }

        public SlotData(float totalMental,
                        float totalPhysical,
                        float totalExhaustion,
                        int totalDuration,
                        float avgMental,
                        float avgPhysical,
                        float avgExhaustion) {
            this.totalMental = totalMental;
            this.totalPhysical = totalPhysical;
            this.totalExhaustion = totalExhaustion;
            this.totalDuration = totalDuration;
            this.avgMental = avgMental;
            this.avgPhysical = avgPhysical;
            this.avgExhaustion = avgExhaustion;
        }

        public float getTotalMental() {
            return totalMental;
        }

        public void setTotalMental(float totalMental) {
            this.totalMental = totalMental;
        }

        public float getTotalPhysical() {
            return totalPhysical;
        }

        public void setTotalPhysical(float totalPhysical) {
            this.totalPhysical = totalPhysical;
        }

        public float getTotalExhaustion() {
            return totalExhaustion;
        }

        public void setTotalExhaustion(float totalExhaustion) {
            this.totalExhaustion = totalExhaustion;
        }

        public int getTotalDuration() {
            return totalDuration;
        }

        public void setTotalDuration(int totalDuration) {
            this.totalDuration = totalDuration;
        }

        public float getAvgMental() {
            return avgMental;
        }

        public void setAvgMental(float avgMental) {
            this.avgMental = avgMental;
        }

        public float getAvgPhysical() {
            return avgPhysical;
        }

        public void setAvgPhysical(float avgPhysical) {
            this.avgPhysical = avgPhysical;
        }

        public float getAvgExhaustion() {
            return avgExhaustion;
        }

        public void setAvgExhaustion(float avgExhaustion) {
            this.avgExhaustion = avgExhaustion;
        }
    }
}
