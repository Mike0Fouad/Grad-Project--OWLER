from app.extensions import Mongo as me
from datetime import datetime
from app.models.mongodb.schedule import *
from app.models.mongodb.user_data import *

class Day(me.EmbeddedDocument):
    """
    Day model for representing a day with associated schedule, user data, and last modified timestamp.

    Attributes:
        Date (str): The date of the day in "YYYY-MM-DD" format (e.g., "2025-02-16").
        Schedule (Schedule): The schedule for the day, including tasks and related data.
        UserData (UserData): The user data associated with the day, such as fitness or ML predictions.
        Last_modified (datetime): The timestamp of the last modification to the day's data.

    Example Structure:
    {
        "Date": "2025-02-16",
        "Schedule": {
            "start": "08:00",
            "end": "20:00",
            "done": 0.75,
            "exhaustion": 5,
            "daily_score": 85,
            "tasks": [
                {
                    "name": "Task Name",
                    "start": "08:00",
                    "end": "09:00",
                    "deadline": "09:00",
                    "done": false,
                    "mental": 5,
                    "physical": 5,
                    "exhaustion": 5,
                    "priority": 1
                },
                ...
            ]
        },
        "UserData": {
            "googlefit": {
                "meta_data": {},
                "data": {}
            },
            "schedule_data": {
                "start": "08:00",
                "end": "20:00",
                "daily_score": 85,
                "exhaustion": 5,
                "done": 0.75
            },
            "AggregatedTaskData": {
                "ScheduleData": [...],
                "tasks": [...],
                "start": "08:00",
                "end": "20:00"
            },
            "MLdata": [
                {
                    "time_slot": "08:00-09:00",
                    "predicted_CP": 0.85,
                    "predicted_PE": 0.75
                },
                ...
            ]
        },
        "Last_modified": "2025-02-16T12:34:56Z"
    }
    """
    date = me.StringField(required=True)  # e.g., "2025-02-16"
    schedule = me.EmbeddedDocumentField(Schedule, required=False)
    UserData = me.EmbeddedDocumentField(UserData, required=False)
    Last_modified = me.DateTimeField(default=datetime.now())
    
    def to_dict(self):
        """
        Convert the Day object to a dictionary to make it serializable.
        """
        return {
            "date": self.date,
            "schedule": self.schedule.to_dict() if self.schedule else None,
            "UserData": self.UserData.to_dict() if self.UserData else None,
            "Last_modified": self.Last_modified.isoformat() if self.Last_modified else None
        }

