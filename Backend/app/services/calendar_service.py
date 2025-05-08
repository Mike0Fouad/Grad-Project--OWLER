from datetime import datetime
from app.models.mongodb.user_data import GoogleFitData, GoogleFitMetaData, HourlyMetric, SleepStageData, TimeFeatures
from app.repositories import CalendarRepository, UserRepository
from app.models.mongodb.day import Day  # Ensure Day is defined appropriately

class CalendarService:
    """
    Service class for managing user calendars, including operations
    such as retrieving, updating, and removing calendar data.
    """
    def __init__(self):
        """
        Initialize the CalendarService with a CalendarRepository instance.
        """
        self.repo = CalendarRepository()
        self.user_repo = UserRepository()
    
    #--------------------------------
    # Getter Methods
    #--------------------------------
    # def get_calendar_for_user(self, user_id: str):
    #     """
    #     Retrieve the calendar data for a specific user.

    #     Args:
    #         user_id (str): The unique identifier of the user.

    #     Returns:
    #         dict: The calendar data for the user, or None if not found.
    #     """
    #     return self.repo.get_calendar(user_id)

    def get_day(self, user_id: str, date_str: str) -> dict:
        """
        Retrieve a specific day from the user's calendar.

        Args:
            user_id (str): The unique identifier of the user.
            date_str (str): The date in 'YYYY-MM-DD' format of the day to retrieve.

        Returns:
            Day: An instance of the Day model representing the specified day, or None if not found.
        """
        return self.repo.get_day(user_id, date_str)
    
    def get_user_data_for_day(self, user_id: str, date_str: str) -> dict:
        """
        Retrieve user data for a specific day.

        Args:
            user_id (str): The unique identifier of the user.
            date_str (str): The date in 'YYYY-MM-DD' format for which to retrieve data.

        Returns:
            dict: A dictionary containing the user data for the specified day, or None if not found.
        """
        return self.repo.get_UserData(user_id, date_str)

    def get_all_user_data(self, user_id: str) -> list:
        """
        Retrieve all user data from the calendar.

        Args:
            user_id (str): The unique identifier of the user.

        Returns:
            list: A list of all user data entries for the specified user.
        """
        return self.repo.get_all_UserData(user_id)
    
    def get_google_fit_data_for_day(self, user_id: str, date_str: str) -> dict:
        """
        Retrieve Google Fit data for a specific day.

        Args:
            user_id (str): The unique identifier of the user.
            date_str (str): The date in 'YYYY-MM-DD' format for which to retrieve Google Fit data.

        Returns:
            dict: A dictionary containing the Google Fit data for the specified day, or None if not found.
        """
        return self.repo.get_google_fit_data(user_id, date_str)
    
    def get_ml_data_for_day(self, user_id: str, date_str: str) -> list:
        """
        Retrieve machine learning (ML) data for a specific day.

        Args:
            user_id (str): The unique identifier of the user.
            date_str (str): The date in 'YYYY-MM-DD' format for which to retrieve ML data.

        Returns:
            list: A list containing the ML data for the specified day, or None if not found.
        """
        return self.repo.get_ml_data(user_id, date_str)
    
    
    
    #--------------------------------
    # Setter Methods
    #--------------------------------
    
    def add_or_update_day_schedule(self, user_id: str, day: Day) -> bool:
        """
        Add or update a specific day in the user's calendar.

        Args:
            user_id (str): The unique identifier of the user.
            day (Day): An instance of the Day model containing the day's data.

        Returns:
            bool: True if the operation is successful; otherwise, False.
        """
        try:
            self.repo.update_day_schedule(user_id, day)
            return True
        except Exception as e:
            print(f"Error adding/updating day schedule: {e}")
            return False

    def update_google_fit_data(self, user_id, fit_json,date_str) -> bool:
        fit_doc = GoogleFitData(
            meta_data=GoogleFitMetaData(
                user_id=user_id,
                collected_at=datetime.fromisoformat(fit_json['last_updated']),
            ),
            hourly_metrics=[
                HourlyMetric(
                    hour_range=m['hour_range'],
                    steps=m['steps'],
                    heart_rate=m['heart_rate'],
                    time_features=TimeFeatures(**m['time_features'])
                ) for m in fit_json['hourly_metrics']
            ],
            sleep=SleepStageData(**fit_json['sleep']),
            hrv=fit_json.get('hrv', 0.0),
            last_updated=datetime.fromisoformat(fit_json['last_updated'])
        )
        return self.repo.update_google_fit_data(user_id, date_str, fit_doc.to_dict())

    def update_ml_data(self, user_id: str, date_str: str, ml_data: list) -> bool:
        """
        Update machine learning (ML) data for a specific day in the user's calendar.

        Args:
            user_id (str): The unique identifier of the user.
            date_str (str): The date in 'YYYY-MM-DD' format for which to update ML data.
            ml_data (list): A list containing ML data to be updated for the specified day.

        Returns:
            bool: True if the update is successful; otherwise, False.
        """
        return self.repo.update_ml_predictions(user_id, date_str, ml_data)

    
    
    #--------------------------------
    # Deletion Methods
    #--------------------------------
    def remove_day(self, user_id: str, date_str: str) -> bool:
        """
        Remove a specific day from the user's calendar.

        Args:
            user_id (str): The unique identifier of the user.
            date_str (str): The date in 'YYYY-MM-DD' format of the day to be removed.

        Returns:
            bool: True if the removal is successful; otherwise, False.
        """
        return self.repo.remove_day(user_id, date_str)

    #--------------------------------
    # Processes Methods
    #--------------------------------
    def state_schedule_data_for_all_users(self, date_str: str) -> bool:
        """
        State the schedule data for all users.

        Args:
            date_str (str): The date in 'YYYY-MM-DD' format for which to state the schedule data.

        Returns:
            bool: True if the operation is successful; otherwise, False.
        """
        try:
            user_ids = self.user_repo.get_all_users_id()
            for user_id in user_ids:
                self.repo.state_schedule_data(user_id, date_str)
            return True
        except Exception as e:
            print(f"Error stating schedule data for all users: {e}")
            return False
        
    def state_all_schedule_data_for_all_users(self) -> bool:
        """
        State all schedule data for all users.

        Returns:
            bool: True if the operation is successful; otherwise, False.
        """
        try:
            user_ids = self.user_repo.get_all_users_id()
            for user_id in user_ids:
                self.repo.state_all_schedule_data(user_id)
            return True
        except Exception as e:
            print(f"Error stating all schedule data: {e}")
            return False
        
        
        