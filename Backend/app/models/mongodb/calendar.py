from app.extensions import Mongo as me
from datetime import datetime
from app.models.mongodb.day import Day  # Assumes Day is an EmbeddedDocument with fields: date, user_data, etc.

class Calendar(me.Document):
    """
    Calendar model for storing a user's calendar data.

    Attributes:
        user_id (str): The unique identifier for the user (linked to MySQL user data).
        days (list[Day]): A list of Day objects representing the user's daily data.
    --------------------
    Structure example: {
        "user_id": "12345",
        "days": [
            {},{},{},
            ...
        ]
    }
    """
    
    user_id = me.StringField(required=True, unique=True)  # Links with secure user data in MySQL
    days = me.EmbeddedDocumentListField(Day)
    
    meta = {
        'collection': 'calendars'
    }
    #-------------------------------------------------------------------
    # Set user_id (to link MySQL user_id with MongoDB)
    #-------------------------------------------------------------------
    def set_user_id(self, user_id) -> bool:
        """
        Set the user ID for the calendar.

        Args:
            user_id (str): The unique identifier for the user.
        """
        self.user_id = user_id
        return True
        
    #--------------------------------
    # Days methods
    #--------------------------------
    def get_day(self, date_str) -> dict:
        """Retrieve a day from the calendar by its date.

        Args:
            date_str (str): The date of the day to retrieve in 'YYYY-MM-DD' format.

        Returns:
            list[Day]: A list containing the matching Day object(s) if found, otherwise an empty list.
        """
        Days = [day.to_dict() for day in self.days if date_str == day.Date]
        if len(Days) == 0:
            return None
        elif len(Days) == 1:
            return Days[0].get
        else:
            return Days
    
    
    def add_or_update_day_schedule(self, day) -> bool:
        """Add or update a day's schedule in the calendar.

        Args:
            day (Day): The Day object to add or update in the calendar.

        Returns:
            bool: True if the day's schedule was added or updated successfully, False otherwise.
        """
        existing_day = self.get_day(day['date'])
        if existing_day and existing_day.Last_modified > day.Last_modified:
            # Update the existing day's schedule
            existing_day.Schedule = day.Schedule
            existing_day.Last_modified = datetime.now()
            return True
        elif existing_day and existing_day.Last_modified <= day.Last_modified:
            # Replace the existing day with the new one
            return False
        elif existing_day is None:
            # Add the new day to the calendar
            self.days.append(day)
            return True
        return False
            
        """Retrieve the schedule for a specific day in the calendar.

        Args:
            date_str (str): The date of the day to retrieve in 'YYYY-MM-DD' format.

        Returns:
            Day: The Day object containing the schedule for the specified date, or None if not found.
        """
        days = self.get_day(date_str)
        if days:
            # Assuming one day per date; return the first matching day's schedule.
            return days[0].schedule
        return None
    
    def remove_day(self, date_str) -> bool:
        """Remove a day from the calendar by its date.

        Args:
            date_str (str): The date of the day to remove in 'YYYY-MM-DD' format.

        Returns:
            bool: True if a day was removed, False otherwise.
        """
        original_count = len(self.days)
        self.days.delete(date=date_str)
        if len(self.days) != original_count:
            self.save()
            return True
        return False
    #--------------------------------
    # User Data methods
    #--------------------------------
    def get_user_data_for_day(self, date_str) -> dict:
        """Update the user data for a specific day in the calendar.

        Args:
            date_str (str): The date of the day to update in 'YYYY-MM-DD' format.
            new_user_data (dict or UserData): The new user data to update. Can be a dictionary or a UserData object.

        Returns:
            bool: True if the user data was updated successfully, False otherwise.
        """
        days = self.get_day(date_str)
        if days:
            # Assuming one day per date; return the first matching day's user_data as a dict.
            day = days[0]
            if day.UserData:
                return day.UserData.to_dict()
        return None

    def update_google_fit_data_for_current_day(self, fit_data) -> bool:
        """Update Google Fit data for the current day in the calendar.

        Args:
            fit_data (dict): A dictionary containing Google Fit data to update for the current day.

        Returns:
            bool: True if the Google Fit data was updated successfully, False otherwise.
        """
        current_date = datetime.now().strftime("%Y-%m-%d")
        day = self.get_day(current_date)
        
            
        if day.UserData:
            
            day.UserData.GoogleFitData = fit_data
        else:
            # Import UserData from your module (adjust the import path as needed)
            from app.models.mongodb.user_data import UserData
            day.UserData = UserData(fit_data)
        day.Last_modified = datetime.now()
        self.save()
        return False

    def update_user_data_for_day(self, date_str, new_user_data) -> bool:
        """Update the user data for a specific day in the calendar.

        Args:
            date_str (str): The date of the day to update in 'YYYY-MM-DD' format.
            new_user_data (dict or UserData): The new user data to update. Can be a dictionary or a UserData object.

        Returns:
            bool: True if the user data was updated successfully, False otherwise.
        """
        day = self.get_day(date_str)
        if day:
            from app.models.mongodb.user_data import UserData
            
            if isinstance(new_user_data, dict):
                day.user_data = UserData(**new_user_data)
            else:
                day.UserData = new_user_data
            day.Last_modified = datetime.now()
            self.save()
            return True
        return False

    def to_dict(self):
        """
        Convert the Calendar object to a dictionary to make it serializable.
        """
        return {
            'user_id': self.user_id,
            'days': [day.to_dict() for day in self.days]  # Ensure the Day object is also serializable
        }
        
    def save(self, *args, **kwargs):
        """Override save to ensure that user_id is used as calendar_id."""
        self.calendar_id = self.user_id  # Make calendar_id the same as user_id
        super(Calendar, self).save(*args, **kwargs)
        
