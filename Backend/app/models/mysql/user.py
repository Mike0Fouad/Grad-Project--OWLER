from datetime import datetime
import uuid
from app.extensions import mysql  # Assumes extensions.py initializes SQLAlchemy as 'mysql'


class User(mysql.Model):
    """
    User Model:
    Represents a user in the system.

    Attributes:
        id (int): Primary key, unique identifier for the user.
        email (str): User's email address, must be unique.
        name (str): User's full name.
        age (int, optional): User's age.
        gender (str, optional): User's gender.
        password_hash (str, optional): Hashed password for authentication (nullable for OAuth users).
        oauth_id (str): Unique identifier from Google OAuth.
        access_token (str, optional): Access token for OAuth authentication.
        refresh_token (str, optional): Refresh token for OAuth authentication.
        productivity_score (int, optional): User's productivity score.
        created_at (datetime): Timestamp when the user was created.
        updated_at (datetime): Timestamp when the user was last updated.
    """
    __tablename__ = 'users'

    id = mysql.Column(mysql.String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    email = mysql.Column(mysql.String(255), unique=True, nullable=False)
    name = mysql.Column(mysql.String(255), nullable=False)
    age = mysql.Column(mysql.Integer, nullable=True)
    gender = mysql.Column(mysql.String(1), nullable=True)
    password_hash = mysql.Column(mysql.String(255), nullable=True)  # Null for OAuth users
    oauth_id = mysql.Column(mysql.String(255), nullable=False)  # Unique ID from Google OAuth
    access_token = mysql.Column(mysql.Text, nullable=True)  # Access token for OAuth
    refresh_token = mysql.Column(mysql.Text, nullable=True)  # Refresh token for OAuth
    created_at = mysql.Column(mysql.DateTime, default=datetime.now)
    updated_at = mysql.Column(mysql.DateTime, default=datetime.now, onupdate=datetime.now)

    #--------------------------------
    # output User's details
    #--------------------------------
    def to_dict(self)-> dict:
        """Converts the User object into a dictionary representation.

        Returns:
            dict: A dictionary containing the user's details.
        """
        return {
            "id": self.id,
            "email": self.email,
            "name": self.name,
            "age": self.age,
            "gender": self.gender,
            "created_at": self.created_at.isoformat(),
            "updated_at": self.updated_at.isoformat()
        }