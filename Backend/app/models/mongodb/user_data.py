from datetime import datetime
from app.extensions import Mongo as me

#---------------------------------
# Google Fit Data Models
#---------------------------------

class GoogleFitMetaData(me.EmbeddedDocument):
    """
    Metadata about Google Fit data collection
    """
    user_id = me.StringField()
    collected_at = me.DateTimeField()
    processed_at = me.DateTimeField(default=datetime.now)
    
    def to_dict(self):
        """
        Convert the GoogleFitMetaData object to a dictionary representation.
        
        Returns:
            dict: A dictionary representation of the GoogleFitMetaData object.
        """
        return {
            "user_id": self.user_id,
            "collected_at": self.collected_at,
            "processed_at": self.processed_at
        }

class TimeFeatures(me.EmbeddedDocument):
    """
    Cyclical time encoding features
    """
    sin_time = me.FloatField()
    cos_time = me.FloatField()
    
    def to_dict(self):
        """
        Convert the TimeFeatures object to a dictionary representation.
        
        Returns:
            dict: A dictionary representation of the TimeFeatures object.
        """
        return {
            "sin_time": self.sin_time,
            "cos_time": self.cos_time
        }

class HourlyMetric(me.EmbeddedDocument):
    """
    Health metrics for a specific hour slot
    """
    hour_range = me.StringField()  # Format: "08:00-09:00"
    steps = me.IntField(default=0)
    heart_rate = me.FloatField(default=0.0)
    time_features = me.EmbeddedDocumentField(TimeFeatures)
    
    def to_dict(self):
        """
        Convert the HourlyMetric object to a dictionary representation.
        
        Returns:
            dict: A dictionary representation of the HourlyMetric object.
        """
        return {
            "hour_range": self.hour_range,
            "steps": self.steps,
            "heart_rate": self.heart_rate,
            **self.time_features.to_mongo().to_dict()
        }

class SleepStageData(me.EmbeddedDocument):
    """
    Detailed sleep stage metrics
    """
    total_hours = me.FloatField(default=0.0)
    deep_hours = me.FloatField(default=0.0)
    rem_hours = me.FloatField(default=0.0)
    light_hours = me.FloatField(default=0.0)
    awake_episodes = me.IntField(default=0)
    
    def to_dict(self):
        """
        Convert the SleepStageData object to a dictionary representation.
        
        Returns:
            dict: A dictionary representation of the SleepStageData object.
        """
        return {
            "total_hours": self.total_hours,
            "deep_hours": self.deep_hours,
            "rem_hours": self.rem_hours,
            "light_hours": self.light_hours,
            "awake_episodes": self.awake_episodes
        }

class GoogleFitData(me.EmbeddedDocument):
    """
    Complete Google Fit dataset for prediction pipeline
    """
    meta_data = me.EmbeddedDocumentField(GoogleFitMetaData)
    hourly_metrics = me.EmbeddedDocumentListField(HourlyMetric)
    sleep = me.EmbeddedDocumentField(SleepStageData)
    hrv = me.FloatField(default=0.0)  # Heart Rate Variability (RMSSD)
    last_updated = me.DateTimeField(default=datetime.now)
    
    def to_dict(self):
        """
        Convert the GoogleFitData object to a dictionary representation.
        
        Returns:
            dict: A dictionary representation of the GoogleFitData object.
        """
        return {
            "meta_data": self.meta_data.to_mongo().to_dict(),
            "hourly_metrics": [metric.to_mongo().to_dict() for metric in self.hourly_metrics],
            "sleep": self.sleep.to_mongo().to_dict(),
            "hrv": self.hrv,
            "last_updated": self.last_updated
        }

    def to_prediction_format(self):
        """Convert to ML pipeline input format"""
        return {
            'hourly': [
                {
                    'time_slot': metric.hour_range,
                    'steps': metric.steps,
                    'heart_rate': metric.heart_rate,
                    **metric.time_features.to_mongo()
                }
                for metric in self.hourly_metrics
            ],
            'sleep': self.sleep.to_mongo().to_dict(),
            'hrv': self.hrv
        }



#---------------------------------
# MLData Model
#---------------------------------
class MLData(me.EmbeddedDocument):
    """
    MLData model for storing machine learning predictions.

    Attributes:
        time_slot (str): The time slot for the prediction (e.g., "08:00-09:00").
        predicted_CP (float): The predicted cognitive performance (0 to 1).
        predicted_PE (float): The predicted physical energy (0 to 1).
    --------------------
    Structure example: {
        "time_slot": "08:00-09:00",
        "predicted_CP": 0.85,
        "predicted_PE": 0.75
    }
    """
    time_slot = me.StringField()   # e.g., "08:00-09:00"
    predicted_CP = me.FloatField()   # Cognitive performance prediction (0-1)
    predicted_PE = me.FloatField()   # Physical energy prediction (0-1)
    
    def to_dict(self):
        """
        Convert the MLData object to a dictionary representation.
        
        Returns:
            dict: A dictionary representation of the MLData object.
        """
        return {
            "time_slot": self.time_slot,
            "predicted_CP": self.predicted_CP,
            "predicted_PE": self.predicted_PE
        }
#---------------------------------
# Shedule Data Models
#---------------------------------
class AggregatedTaskData(me.EmbeddedDocument):
    """
    AggregatedTaskData model for storing aggregated task information.

    Attributes:
        tasks (EmbeddedDocumentListField): List of tasks associated with the schedule.
        start (str): Start time of the schedule (e.g., "08:00").
        end (str): End time of the schedule (e.g., "20:00").
        slots (dict): Dictionary containing aggregated data for each time slot.
    --------------------
    Structure example: {
        "start": "08:00",
        "end": "20:00",
        "slots": {
            "08:00-09:00": {
                "total_mental": 100,
                "total_physical": 200,
                "total_exhaustion": 50,
                "total_duration": 60,
                "avg_mental": 1.67,
                "avg_physical": 3.33,
                "avg_exhaustion": 0.83
            },
            ...
        }
    }
    """
    
    start = me.StringField(   regex=r'^\d{2}:\d{2}')  # Start time of the schedule as a datetime object
    end = me.StringField(  regex=r'^\d{2}:\d{2}')   # End time of the schedule (e.g., "20:00")
    slots = me.DictField()  # Placeholder for the slots dictionary, to be populated later by a method
    #--------------------------------
    # AggregatedTaskData methods
    #--------------------------------
    def aggregate_by_time_slots(self, start_of_day: str = None, end_of_day: str = None, slot_minutes: int = 60) -> dict:
        """
        Aggregates task data into time slots.

        Args:
            start_of_day (str, optional): Start time of the day in "HH:MM" format. Defaults to the schedule's start time.
            end_of_day (str, optional): End time of the day in "HH:MM" format. Defaults to the schedule's end time.
            slot_minutes (int, optional): Duration of each time slot in minutes. Defaults to 60.

        Returns:
            dict: A dictionary where keys are time slot ranges (e.g., "08:00-09:00") and values contain aggregated task data.
        """
        tasks = me.EmbeddedDocumentListField('Task')
        self.start = start_of_day or self.start
        self.end = end_of_day or self.end
        def time_to_minutes(t_str) -> int:
            """
            Converts a time string in "HH:MM" format to the total number of minutes since midnight.

            Args:
                t_str (str): Time string in "HH:MM" format.

            Returns:
                int: Total minutes since midnight.
            """
            h, m = map(int, t_str.split(":"))
            return h * 60 + m

        start_min = time_to_minutes(start_of_day or self.start)
        end_min = time_to_minutes(end_of_day or self.end)
        
        # Create slots dictionary
        slots = {}
        
        while slot_time < end_min:
            slot_end = slot_time + slot_minutes
            slot_key = "{:02d}:{:02d}-{:02d}:{:02d}".format(slot_time // 60, slot_time % 60,
                                                               slot_end // 60, slot_end % 60)
            slots[slot_key] = {"total_mental": 0, "total_physical": 0, "total_exhaustion": 0, "total_duration": 0}
            slot_time += slot_minutes
            

        # Aggregate task values per slot
        for task in self.tasks:
            task_start = time_to_minutes(task.start_time)
            task_end = time_to_minutes(task.end_time)
            task_duration = task_end - task_start
            if task_duration <= 0:
                continue  # Skip invalid tasks

            for slot_key in slots:
                # Extract slot start and end from the key
                slot_parts = slot_key.split("-")
                slot_start = time_to_minutes(slot_parts[0])
                slot_end = time_to_minutes(slot_parts[1])
                # Calculate overlap between task and slot
                overlap = max(0, min(task_end, slot_end) - max(task_start, slot_start))
                if overlap > 0:
                    proportion = overlap / task_duration
                    slots[slot_key]["total_mental"] += task.mental * overlap
                    slots[slot_key]["total_physical"] += task.physical * overlap
                    slots[slot_key]["total_exhaustion"] += task.exhaustion * overlap
                    slots[slot_key]["total_duration"] += overlap

        # Compute averages for each slot
        for slot_key, data in slots.items():
            if data["total_duration"] > 0:
                data["avg_mental"] = data["total_mental"] / data["total_duration"]
                data["avg_physical"] = data["total_physical"] / data["total_duration"]
                data["avg_exhaustion"] = data["total_exhaustion"] / data["total_duration"]
            else:
                data["avg_mental"] = 0
                data["avg_physical"] = 0
                data["avg_exhaustion"] = 0
        # slots structure example: {
        #     "08:00-09:00": {"total_mental": 100, "total_physical": 200, ...},
        #     "09:00-10:00": {"total_mental": 150, "total_physical": 250, ...},
        self.slots = slots  # Store the slots in the instance variable
        return slots  # Return the slots for further use if needed

    def to_dict(self):
        """
        Convert the AggregatedTaskData object to a dictionary representation.
        
        Returns:
            dict: A dictionary representation of the AggregatedTaskData object.
        """
        return {
            "start": self.start,
            "end": self.end,
            "slots": self.slots
        }
    
    
class ScheduleData(me.EmbeddedDocument):
    """
    ScheduleData model for storing schedule-related information.

    Attributes:
        ScheduleData (EmbeddedDocumentListField): List of schedule data.
        start (str): Overall start time of the schedule (e.g., "08:00").
        end (str): Overall end time of the schedule (e.g., "20:00").
        daily_score (int): Overall daily performance score.
        exhaustion (int): Overall exhaustion rating (0 to 10).
        done (float): Fraction of the schedule completed (0 to 1).
    --------------------
    Structure example: {
        "ScheduleData": [...],
        "start": "08:00",
        "end": "20:00",
        "daily_score": 85,
        "exhaustion": 5,
        "done": 0.75
    }
    """
    
    
    start = me.StringField()           # Overall start time of the schedule (e.g., "08:00")
    end = me.StringField()             # Overall end time (e.g., "20:00")
    daily_score = me.IntField()        # Overall daily performance score
    exhaustion = me.IntField(min_value=0, max_value=10)  # Overall exhaustion rating
    done = me.FloatField()             # Fraction of schedule completed (0 to 1)
    def to_dict(self):
        """
        Convert the ScheduleData object to a dictionary representation.
        
        Returns:
            dict: A dictionary representation of the ScheduleData object.
        """
        return {
            "start": self.start,
            "end": self.end,
            "daily_score": self.daily_score,
            "exhaustion": self.exhaustion,
            "done": self.done
        }
    
    def get_schedule_data(self):
        """
        Returns the schedule data as a list of dictionaries.
        
        Returns:
            list: List of dictionaries containing schedule data.
        """
        try:
            ScheduleData = me.EmbeddedDocumentListField('Schedule')
            self.start = ScheduleData.start or self.start
            self.end = ScheduleData.end or self.end
            self.daily_score = ScheduleData.daily_score or self.daily_score
            self.exhaustion = ScheduleData.exhaustion or self.exhaustion
            self.done = ScheduleData.done or self.done
        except Exception:
            return False  # Return False if any error occurs
        return True
        
#---------------------------------
# User Data Model
#---------------------------------
class UserData(me.EmbeddedDocument):
    """
    UserData model for storing user-related data.

    Attributes:
        googlefit (EmbeddedDocumentField): Google Fit data for the user.
        schedule_data (EmbeddedDocumentField): Schedule-related data for the user.
        AggregatedTaskData (EmbeddedDocumentField): Aggregated task data for the user.
        MLdata (EmbeddedDocumentListField): Machine learning predictions for the user.
    --------------------
    Structure example: {
        "googlefit": {
            "meta_data": {},
            "data": {}
        },
        "schedule_data": {},
        "AggregatedTaskData": {},
        "MLdata": [
            {}
        ]
    }
    """
    GoogleFitData = me.EmbeddedDocumentField(GoogleFitData)
    ScheduleData = me.EmbeddedDocumentField(ScheduleData)
    AggregatedTaskData = me.EmbeddedDocumentField(AggregatedTaskData)
    MLData = me.EmbeddedDocumentListField(MLData)
    def to_dict(self):
        """
        Convert the UserData object to a dictionary representation.
        
        Returns:
            dict: A dictionary representation of the UserData object.
        """
        return {
            "googlefit": self.GoogleFitData.to_mongo().to_dict() if self.GoogleFitData else None,
            "schedule_data": self.ScheduleData.to_mongo().to_dict() if self.ScheduleData else None,
            "AggregatedTaskData": self.AggregatedTaskData.to_mongo().to_dict() if self.AggregatedTaskData else None,
            "MLdata": [data.to_mongo().to_dict() for data in self.MLData] if self.MLData else []
        }
       
