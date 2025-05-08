# user_repository.py
import logging
from app.extensions import mysql
from app.models.mysql import *
logger = logging.getLogger(__name__)

class UserRepository:
    """
    A repository class for performing CRUD operations and managing user-related data.
    """
    def __init__(self, session=None):
        # Use the provided session or the global db.session
        self.session = session or mysql.session

    #--------------------------------
    # User CRUD Operations
    #--------------------------------
    ## create a new user
    def create_user(self, email: str, name: str, password_hash: str, age: int = None, gender: str = None) -> User:
        """
        Creates a new user in the database.

        Args:
            email (str): The email address of the user.
            name (str): The full name of the user.
            password_hash (str): The hashed password of the user.
            age (int, optional): The age of the user. Defaults to None.
            gender (str, optional): The gender of the user. Defaults to None.

        Returns:
            User: The newly created User object.
        """
        user = User(email=email, name=name, password_hash=password_hash, age=age, gender=gender)
        self.session.add(user)
        self.session.commit()
        return user

    ## get user by id
    def get_user_by_id(self, user_id: str) -> User:
        """
        Retrieve a user by their unique ID.

        Args:
            user_id (int): The unique identifier of the user.

        Returns:
            User: The User object if found, otherwise False.
        """
        return self.session.query(User).filter_by(id=user_id).first() or False

    ## get user by email
    def get_user_by_email(self, email: str) -> User:
        """
        Retrieve a user by their email address.

        Args:
            email (str): The email address of the user.

        Returns:
            User: The User object if found, otherwise False.
        """
        return self.session.query(User).filter_by(email=email).first() or False
    
    ## delete a user
    def delete_user(self, user_id: str) -> bool:
        """
        Deletes a user from the database by their unique ID.

        Args:
            user_id (str): The unique identifier of the user to be deleted.

        Returns:
            bool: True if the user was successfully deleted, False otherwise.
        """
        user = self.get_user_by_id(user_id)
        if user:
            self.session.delete(user)
            self.session.commit()
            return True
        return False
    
    ## get user_id by email
    def get_user_id(self, email: str) -> str:
        """
        Retrieve the unique ID of a user by their email address.

        Args:
            email (str): The email address of the user.

        Returns:
            int: The unique ID of the user if found, otherwise None.
        """
        user = self.get_user_by_email(email)
        if user:
            return user.id
        return None
    
    ## get all users id
    def get_all_users_id(self) -> list:
        """
        Retrieve a list of all user IDs in the database.

        Returns:
            list: A list of unique user IDs.
        """
        users = self.session.query(User).all()
        return [user.id for user in users]
    
    ## get all users with google fit data
    def get_all_users_with_google_fit(self) -> list:
        """
        Retrieve a list of all users who have Google Fit data.

        Returns:
            list: A list of User objects with Google Fit data.
        """
        users = self.session.query(User).filter(User.oauth_id.isnot(None)).all()
        return users
    
    
    #-------------------------------- 
    # User Profile Operations
    #--------------------------------
    ## update user profile
    def update_user_profile(self, user_id: str, **kwargs) -> User:
        """
        Updates the profile of a user with the given attributes.

        Args:
            user_id (int): The unique identifier of the user to be updated.
            **kwargs: Key-value pairs of attributes to update (e.g., name, age, gender).

        Returns:
            User: The updated User object if the user exists, otherwise None.
        """
        user = self.get_user_by_id(user_id)
        if user:
            for key, value in kwargs.items():
                if hasattr(user, key):
                    setattr(user, key, value)
            self.session.commit()
            return user
        return None
    

    
    #--------------------------------
    # Token Operations 
    #--------------------------------
    ## save tokens
    def save_tokens(self, user_id: str, access_token: str, refresh_token: str) -> bool:
        """
        Saves the access and refresh tokens for a user.

        Args:
            user_id (int): The unique identifier of the user.
            access_token (str): The access token to be saved.
            refresh_token (str): The refresh token to be saved.

        Returns:
            bool: True if the tokens were successfully saved, False otherwise.
        """
        user = self.get_user_by_id(user_id)
        if user:
            user.access_token = access_token
            user.refresh_token = refresh_token
            self.session.commit()
            return True
        return False

    ## get tokens
    def get_tokens(self, user_id: str) -> dict:
        """
        Retrieves the JWT and refresh tokens for a user.

        Args:
            user_id (int): The unique identifier of the user.

        Returns:
            dict: A dictionary containing the JWT token and refresh token if the user exists, otherwise None.
        """
        user = self.get_user_by_id(user_id)
        if user:
            return {
                "access_token": user.access_token,
                "refresh_token": user.refresh_token
            }
        return None
    
