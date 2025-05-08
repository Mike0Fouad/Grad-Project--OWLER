from app.extensions import Mongo as me

class Task(me.EmbeddedDocument):
    """
    Task model for the schedule.

    Attributes:
        name (str): The name of the task.
        start (str): The start time of the task (e.g., "08:00").
        end (str): The end time of the task (e.g., "09:00").
        deadline (str): The deadline time of the task (e.g., "09:00").
        done (bool): Whether the task is completed.
        mental (int): The mental workload rating (1 to 10).
        physical (int): The physical workload rating (1 to 10).
        exhaustion (int): The exhaustion level (1 to 10).
        priority (int): The priority level of the task.
    --------------------
    Structure example: {
        "name": "Task Name",
        "start": "08:00",
        "end": "09:00",
        "deadline": "09:00",
        "done": false,
        "mental": 5,
        "physical": 5,
        "exhaustion": 5,
        "priority": 1
    }
    """
    
    name = me.StringField(required=True)
    start = me.StringField(required=True)      # e.g., "08:00"
    end = me.StringField(required=True)        # e.g., "09:00"
    deadline = me.StringField(required=True)   # e.g., "09:00"
    done = me.BooleanField(default=False)
    mental = me.IntField(min_value=1, max_value=10)    # Rating for mental workload
    physical = me.IntField(min_value=1, max_value=10)  # Rating for physical workload
    exhaustion = me.IntField(min_value=0, max_value=10)
    priority = me.IntField(required=True)
    
    
        
    
    #--------------------------------
    # Calculate duration in minutes
    #--------------------------------
    def duration_minutes(self) -> int:
        '''
        Convert time strings to minutes since midnight
        '''
        start_h, start_m = map(int, self.start_time.split(":"))
        end_h, end_m = map(int, self.end_time.split(":"))
        return (end_h * 60 + end_m) - (start_h * 60 + start_m)
    
    def to_dict(self):
        """
        Convert the Task object to a dictionary representation.
        
        Returns:
            dict: A dictionary representation of the Task object.
        """
        return {
            "name": self.name,
            "start": self.start,
            "end": self.end,
            "deadline": self.deadline,
            "done": self.done,
            "mental": self.mental,
            "physical": self.physical,
            "exhaustion": self.exhaustion,
            "priority": self.priority
        }
    
    

class Schedule(me.EmbeddedDocument):
    """
    Schedule model for the daily schedule.

    Attributes:
        start (str): The start time of the schedule (e.g., "08:00").
        end (str): The end time of the schedule (e.g., "20:00").
        done (float): The fraction of tasks completed (e.g., 0.75).
        exhaustion (int): The exhaustion level (1 to 10).
        daily_score (int): The daily performance score.
        tasks (list[Task]): A list of tasks in the schedule.
    --------------------
    Structure example: {
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
    }

    
    """
    
    start = me.StringField()  # e.g., "08:00"
    end = me.StringField()    # e.g., "20:00"
    done = me.FloatField()                   # Fraction (e.g., 0.75)
    exhaustion = me.IntField(min_value=0, max_value=10)
    daily_score = me.IntField()
    tasks = me.EmbeddedDocumentListField(Task)
    
    def to_dict(self):
        """
        Convert the Schedule object to a dictionary representation.
        
        Returns:
            dict: A dictionary representation of the Schedule object.
        """
        return {
            "start": self.start,
            "end": self.end,
            "done": self.done,
            "exhaustion": self.exhaustion,
            "daily_score": self.daily_score,
            "tasks": [task.to_dict() for task in self.tasks]
        } if self.tasks else {}