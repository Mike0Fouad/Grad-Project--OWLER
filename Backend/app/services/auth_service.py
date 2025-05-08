import datetime
import logging
import requests
from werkzeug.security import generate_password_hash, check_password_hash
from google.auth.transport import requests as google_requests
from google.oauth2 import id_token as google_id_token
from flask_jwt_extended import create_access_token, create_refresh_token, get_jwt_identity
from app.models.mysql.user import User
from app.repositories import UserRepository, CalendarRepository
from flask import current_app

logger = logging.getLogger(__name__)

class AuthService:
    """
    AuthService provides methods for user authentication and registration,
    including email/password login, Google OAuth2 integration, and JWT token management.
    """
    def __init__(self):
        """
        Initializes the AuthService with user and calendar repositories.
        """
        self.user_repo = UserRepository()
        self.calendar_repo = CalendarRepository()

    
    #--------------------------------
    # Email & Passwords auth
    #--------------------------------
    def register_user(self, data)-> tuple:
        """
        Registers a new user with the provided data.

        Args:
            data (dict): A dictionary containing user details such as email, name, password, age, and gender.
        json data structure:
        {
            "email": "",
            "name": "",
            "password": "",
            "age": "",
            "gender": ""
        }
        Returns:
            tuple: A dictionary with a success message, JWT tokens, and user details, along with an HTTP status code.
        """
        email = data.get("email")
        name = data.get("name")
        password = data.get("password")
        age = data.get("age")
        age = int(age) if isinstance(age,int) or isinstance(age,float) and isinstance(age, str) else int(age)
        gender = data.get("gender")

        # Validate required fields
        if not email or not password or not name:
            return {"msg": "Missing required fields"}, 400

        if self.user_repo.get_user_by_email(email):
            return {"msg": "User already exists"}, 409

        # Hash the password
        password_hash = generate_password_hash(password)

        # Create the user in MySQL
        user = self.user_repo.create_user(email=email, name=name, password_hash=password_hash, age=age, gender=gender)

        # Create the Calendar in MongoDB for the new user
        self.calendar_repo.create_calendar(user.id)
        if not self.calendar_repo.get_calendar(user.id).to_dict():
            return {"msg": "Failed to create calendar"}, 500

        # Issue JWT tokens
        access_token = create_access_token(identity=user.id)
        refresh_token = create_refresh_token(identity=user.id)

        return {
            "msg": "User registered successfully",
            "access_token": access_token,
            "refresh_token": refresh_token,
            "user": user.to_dict()
        }, 201

    def login_user(self, data) -> tuple:
        """
        Authenticates a user using their email and password.

        Args:
            data (dict): A dictionary containing 'email' and 'password' keys.

        Returns:
            tuple: A dictionary with a success message, JWT tokens, and user details, 
                   or an error message with an HTTP status code.
        """
        email = data.get("email")
        password = data.get("password")

        if not email or not password:
            return {"msg": "Missing email or password"}, 400

        user = self.user_repo.get_user_by_email(email)
        if not user or not check_password_hash(user.password_hash, password):
            return {"msg": "Bad email or password"}, 401

        # Issue tokens on successful authentication
        access_token = create_access_token(identity=user.id)
        refresh_token = create_refresh_token(identity=user.id)

        return {
            "msg": "Login successful",
            "access_token": access_token,
            "refresh_token": refresh_token,
            "calendar": self.calendar_repo.get_calendar(user.id).to_dict(),
            "user": user.to_dict()
        }, 200
    
    #--------------------------------
    # Google Auth
    #--------------------------------
    def get_google_oauth_url(self) -> str:
        """
        Generates the Google OAuth2 URL for user authentication.

        Returns:
            str: The Google OAuth2 URL for initiating the authentication process.
        """
        client_id = current_app.config.get("OAUTH_CLIENT_ID")
        redirect_uri = current_app.config.get("OAUTH_REDIRECT_URI")#current_app.config.get("OAUTH_REDIRECT_URI")
        scope = current_app.config.get("GOOGLE_FIT_SCOPES")

        oauth_url = (
            f"https://accounts.google.com/o/oauth2/v2/auth?"
            f"client_id={client_id}&"
            f"redirect_uri={redirect_uri}&"
            f"response_type=code&"
            f"scope={scope}&"
            f"access_type=offline&"
            f"prompt=consent"
        )
        return oauth_url

    def handle_google_callback(self, code) -> tuple:
        """
        Handles the Google OAuth2 callback by exchanging the authorization code for tokens,
        verifying the ID token, and creating or updating the user in the database.

        Args:
            code (str): The authorization code received from Google OAuth2.

        Returns:
            tuple: A dictionary containing a success message, JWT tokens, and user details,
                   or an error message with an HTTP status code.
        """
        if not code:
            return {"msg": "Missing authorization code"}, 400

        client_id = current_app.config.get("OAUTH_CLIENT_ID")
        client_secret = current_app.config.get("OAUTH_CLIENT_SECRET")
        redirect_uri = current_app.config.get("OAUTH_REDIRECT_URI")

        token_url = "https://oauth2.googleapis.com/token"
        token_data = {
            "code": code,
            "client_id": client_id,
            "client_secret": client_secret,
            "redirect_uri": redirect_uri,
            "grant_type": "authorization_code"
        }
        token_resp = requests.post(token_url, data=token_data)
        if token_resp.status_code != 200:
            return {"msg": "Failed to exchange token", "error": token_resp.json()}, 400

        tokens = token_resp.json()
        access_token_google = tokens.get("access_token")
        refresh_token_google = tokens.get("refresh_token")
        id_token = tokens.get("id_token")

        # Verify ID token and extract user info
        token_info_url = f"https://oauth2.googleapis.com/tokeninfo?id_token={id_token}"
        token_info_resp = requests.get(token_info_url)
        if token_info_resp.status_code != 200:
            return {"msg": "Failed to verify id_token", "error": token_info_resp.json()}, 400

        token_info = token_info_resp.json()
        email = token_info.get("email")
        name = token_info.get("name")

        if not email or not name:
            return {"msg": "Insufficient user info from Google"}, 400

        # Check if user exists; if not, create a new one
        user = self.user_repo.get_user_by_email(email)
        if not user:
            user = self.user_repo.create_user(email=email, name=name, password_hash=None)
            user.oauth_provider = "google"
            user.oauth_id = token_info.get("sub")
            self.user_repo.save_tokens(user.id, access_token_google, refresh_token_google)  # Save tokens

            # Create a Calendar for the new user
            self.calendar_repo.create_calendar(user.id)

        else:
            # Update tokens if necessary
            self.user_repo.save_tokens(user.id, access_token_google, refresh_token_google)  # Update tokens

        # Issue JWT tokens
        app_access_token = create_access_token(identity=user.id)
        app_refresh_token = create_refresh_token(identity=user.id)

        return {
            "msg": "Google authentication successful",
            "access_token": app_access_token,
            "refresh_token": app_refresh_token,
            "user": user.to_dict()
        }, 200
    
    

    def handle_google_signin(self, data):
        # 1. Pull out the raw ID token string
        id_token_str = data.get("id_token")
        if not id_token_str:
            return {"error": "Missing ID token"}, 400

        # 2. Verify it came from Google and is intended for our CLIENT_ID
        try:
            payload = google_id_token.verify_oauth2_token(
                id_token_str,
                google_requests.Request(),
                current_app.config["OAUTH_CLIENT_ID"]
            )
        except ValueError:
            return {"error": "Invalid ID token"}, 401

        # 3. Extract the user info directly from the payload
        user_id   = payload.get("sub")
        email     = payload.get("email")
        full_name = payload.get("name")   # optional

        if not email or not user_id:
            return {"error": "Required user info missing in token"}, 400

        # 4. Lookup or create the user record
        user = self.user_repo.get_user_by_email(email)
        if not user:
            user = self.user_repo.create_user(
                email=email,
                name=full_name,
                password_hash=None
            )
            self.calendar_repo.create_calendar(user.id)
            
            user.oauth_id       = user_id
            self.user_repo.save(user)

        # 5. Issue your own JWTs
        app_access_token  = create_access_token(user.id)
        app_refresh_token = create_refresh_token(user.id)

        # 6. Return session tokens and any other needed data
        return {
            "message":       "Google Sign-In successful",
            "access_token":  app_access_token,
            "refresh_token": app_refresh_token,
            "calendar":      self.calendar_repo.get_calendar(user.id).to_dict(),
            "email":         email
        }, 200
    #--------------------------------
    # JWT token
    #--------------------------------
    def refresh_access_token(self) -> tuple:
        """Generates a new access token for the current user.

        Returns:
            tuple: A dictionary containing the new access token and an HTTP status code.
        """
        current_user = get_jwt_identity()
        new_access_token = create_access_token(identity=current_user)
        new_refresh_token = create_refresh_token(identity=current_user)
        return {"access_token": new_access_token,
                "refresh_token":new_refresh_token}, 200
