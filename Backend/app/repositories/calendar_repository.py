import logging
from datetime import datetime, timezone
from typing import Optional, Dict, List
from app.extensions import Mongo
from app.models.mongodb import * # Import all models
logger = logging.getLogger(__name__)

class CalendarRepository:
    """Enhanced calendar repository with proper error handling and type safety"""
    
    def __init__(self):
        self.logger = logging.getLogger(f"{__name__}.{self.__class__.__name__}")

    # --------------------------------
    # Calendar CRUD Operations
    # --------------------------------
    def create_calendar(self, user_id: str) -> dict:
        """Create a new calendar with validation"""
        try:
            calendar = Calendar(user_id=user_id, days=[])
            calendar.save()
            return calendar.to_dict()
        except Exception as e:
            self.logger.error(f"Calendar creation failed for {user_id}: {str(e)}")
            return None
        
    def get_calendar(self, user_id: str) -> Calendar:
        """Safe calendar retrieval with error handling"""
        try:
            cal=Calendar.objects(user_id=user_id).first()
            return cal if cal else None
        except Exception as e:
            self.logger.error(f"Calendar retrieval failed for {user_id}: {str(e)}")
            return None
        
    def delete_calendar(self, user_id: str) -> bool:
        """Delete a calendar by user ID"""
        try:
            calendar = Calendar.objects(user_id=user_id).first()
            if calendar:
                calendar.delete()
                return True
            return False
        except Exception as e:
            self.logger.error(f"Calendar deletion failed for {user_id}: {str(e)}")
            return False

    # --------------------------------
    # Day CRUD Operations
    # --------------------------------
    
    def get_day(self, user_id: str, date_str: str) -> Optional[dict]:
        """Type-safe day retrieval"""
        cal = self.get_calendar(user_id)
        if not cal:
            print(f"Calendar not found for user: {user_id}")
            return None
        for d in cal.days:
            print(f"Checking day: {d.date} against {date_str}")  # Access 'date' as an attribute
            if d.date == date_str:  # Access 'date' as an attribute
                print(f"Found day: {d}")
                return d.to_dict() if d else {}  # Access 'schedule' as an attribute
        return None
    
    def add_or_update_day(self, user_id: str, day: Day) -> bool:
            """Atomic day update with transaction support"""
            try:
                cal = Calendar.objects(user_id=user_id).first() or Calendar(user_id=user_id, days=[])
                #[d for d in cal.days if d.Date != day.Date]
                
                UserData = None
                for d in cal.days:
                    if d.date == day.date:
                        UserData = d.UserData
                        cal.days.remove(d)
                        day.UserData = UserData
                        break
                
                cal.days.append(day)
                cal.save()
                return True
            except Exception as e:
                self.logger.error(f"Failed add_or_update_day: {e}")
                return False
            
    def update_day_schedule(self, user_id: str, day: Day) -> bool:
        return self.add_or_update_day(user_id, day)
    
    def get_day_schedule(self, user_id: str, date_str: str) -> dict:
        """Retrieve the schedule for a specific day"""
        try:
            calendar = self.get_calendar(user_id)
            if not calendar:
                return None
            day = calendar.get_day(date_str)
            if not day or not day.schedule:
                return None
            return day.schedule.to_dict()
        except Exception as e:
            self.logger.error(f"Schedule retrieval failed for {date_str}: {str(e)}")
            return None
    
    def remove_day(self, user_id: str, date_str: str) -> bool:
        """Remove a day from the calendar by its date"""
        try:
            calendar = self.get_calendar(user_id)
            if not calendar:
                return False
            original_count = len(calendar.days)
            calendar.remove_day(date_str)
            if len(calendar.days) != original_count:
                calendar.save()
                return True
            return False
        except Exception as e:
            self.logger.error(f"Day removal failed for {date_str}: {str(e)}")
            return False
    
    # --------------------------------
    # User Data Operations 
    # --------------------------------
    
    # User Data operations
    def get_UserData(self, user_id: str, date_str: str) -> Optional[UserData]:
        """Retrieve user data for a specific date"""
        try:
            cal = self.get_calendar(user_id)
            if not cal:
                return None
            for day in cal.get('days', []):
                if day.get('date') == date_str:
                    if day.get('UserData'):
                        return day['UserData'].to_dict()
            return None
            
            
            
        except Exception as e:
            self.logger.error(f"User data retrieval failed for {date_str}: {str(e)}")
            return None
        
    def get_all_UserData(self, user_id: str) -> List[UserData]:
        """Retrieve all user data for the given user ID"""
        try:
            calendar = self.get_calendar(user_id)
            if not calendar:
                return []
            
            return [day.UserData for day in calendar.days if day and day.UserData and isinstance(day.UserData, UserData) and day.UserData.to_mongo() is not None]
        except Exception as e:
            self.logger.error(f"Failed to retrieve all user data for {user_id}: {str(e)}")
            return []
    
    # Google Fit Data Operations
    def update_google_fit_data(self, user_id: str, date_str: str, fit_data: GoogleFitData) -> bool:
        """Type-safe Google Fit update with schema validation"""
        try:
            calendar = self.get_calendar(user_id)
            if not calendar:
                self.logger.error(f"Calendar not found: {user_id}")
                return False

            day = calendar.get_day(date_str) or Day(date=date_str)
            day.UserData = day.UserData or UserData()
            day.UserData.GoogleFitData = fit_data
            day.Last_modified = datetime.now()
            
            calendar.days.append(day)
            calendar.save()
            return True
        except Mongo.ValidationError as e:
            self.logger.error(f"Data validation failed: {str(e)}")
            return False
        except Exception as e:
            self.logger.error(f"Google Fit update failed: {str(e)}")
            return False
    
    def get_google_fit_data(self, user_id: str, date_str: str) -> Optional[GoogleFitData]:
        """Type-safe Google Fit data retrieval"""
        try:
            calendar = self.get_calendar(user_id)
            if not calendar:
                return None

            day = calendar.get_day(date_str)
            if not day or not day['UserData'] or not day['UserData']['GoogleFitData']:
                return None

            return day.UserData.GoogleFitData
        except Exception as e:
            self.logger.error(f"Google Fit data retrieval failed: {str(e)}")
            return None

    # Schedule Data & Tasks Data Operations
    def state_schedule_data(self, user_id: str, date_str: str) -> bool:
        """Retrieve and populate ScheduleData and AggregatedTaskData for the given date."""
        try:
            calendar = self.get_calendar(user_id)
            if not calendar:
                return None

            day = calendar.get_day(date_str)
            if not day:
                return None
            schedule_start = day.Schedule['start'] if day.Schedule else "08:00"
            schedule_end = day.Schedule['end'] if day.Schedule else "20:00"
            schedule_daily_score = day.Schedule['daily_score'] if day.Schedule else 0
            Schedule_exhaustion = day.Schedule['exhaustion'] if day.Schedule else 0
            schedule_done = day.Schedule['done'] if day.Schedule else 0.0
            
            
            # Ensure UserData exists
            day.UserData = day.UserData or UserData()

            # Populate ScheduleData
            if not day.UserData.schedule_data:
                day.UserData.ScheduleData = ScheduleData(
                    start=schedule_start,
                    end=schedule_end,
                    daily_score=schedule_daily_score,
                    exhaustion=Schedule_exhaustion,
                    done=schedule_done
                )

            # Populate AggregatedTaskData
            if not day.UserData.AggregatedTaskData:
                aggregated_task_data = AggregatedTaskData(
                    start=schedule_start,
                    end=schedule_end)
                aggregated_task_data.aggregate_by_time_slots()
                day.UserData.AggregatedTaskData = aggregated_task_data

            # Save the updated calendar
            calendar.save()
            return True
        except Exception as e:
            self.logger.error(f"Failed to retrieve or populate schedule data for {user_id} on {date_str}: {str(e)}")
            return None
    
    def state_all_schedule_data(self, user_id: str) -> bool:
        """Retrieve and populate ScheduleData and AggregatedTaskData for all days."""
        try:
            calendar = self.get_calendar(user_id)
            if not calendar:
                return None

            for day in calendar.days:
                if not day.UserData:
                    day.UserData = UserData()
                
                if not day.UserData.ScheduleData and not day.UserData.AggregatedTaskData:
                    self.state_schedule_data(user_id, day.date)

            # Save the updated calendar
            calendar.save()
            return True
        except Exception as e:
            self.logger.error(f"Failed to retrieve or populate schedule data for {user_id}: {str(e)}")
            return None
    
    # ML Data Operations
    def update_ml_predictions(self, user_id: str, date_str: str, predictions: List[Dict]) -> bool:
        """ML data update with timezone awareness"""
        try:
            calendar = self.get_calendar(user_id)
            if not calendar:
                return False

            day = calendar.get_day(date_str)[0]
            if not day:
                day = Day(date=date_str)
                calendar.days.append(day)

            day.UserData = day.UserData or UserData()
            day.UserData.MLData = [
                MLData(time_slot=pred["time_slot"],
                    predicted_CP=pred["CP"],
                    predicted_PE=pred["PE"]
                )
                 for pred in predictions
            ]
            day.Last_modified = datetime.now()
            calendar.save()
            return True
        except Exception as e:
            self.logger.error(f"ML prediction update failed: {str(e)}")
            return False
    
    def get_ml_data(self, user_id: str, date_str: str) -> Dict:
        """Structured data for ML pipeline with error handling"""
        try:
            calendar = self.get_calendar(user_id)
            if not calendar:
                return None

            day = calendar.get_day(date_str)
            if not day or not day.UserData or not day.UserData.D:
                return None
            
            ml_data = day.UserData.D
            
            return ml_data
        except Exception as e:
            self.logger.error(f"ML data retrieval failed: {str(e)}")
            return None
