import os

class Config(object):
    """
    Configuration class for the Flask application.

    This class contains configuration variables for Flask, SQLAlchemy, PyMongo, JWT, 
    and OAuth2/Google Fit integration. Default values are provided, but they can be 
    overridden by environment variables.
    """
    # General Flask Configuration
    SECRET_KEY = os.environ.get("SECRET_KEY") or "your-default-secret-key"
    FLASK_ENV = os.environ.get("FLASK_ENV") or "development"
    
    # SQLAlchemy (MySQL) Configuration
    SQLALCHEMY_DATABASE_URI = os.environ.get("SQLALCHEMY_DATABASE_URI") or \
        'mysql+pymysql://Michael42Fouad:qMpaHhJRE[KKebaS@127.0.0.1:3306/owler'
    SQLALCHEMY_TRACK_MODIFICATIONS = False

    # PyMongo (MongoDB) Configuration
    MONGO_URI = os.environ.get("MONGO_URI") or "mongodb://localhost:27017/your_db_name"

    # JWT Configuration
    JWT_SECRET_KEY = os.environ.get("JWT_SECRET_KEY") or "your-jwt-secret-key"
    JWT_ACCESS_TOKEN_EXPIRES = int(os.environ.get("JWT_ACCESS_TOKEN_EXPIRES", 3600))  # seconds

    # OAuth2 / Google Fit Configuration
    OAUTH_CLIENT_ID = os.environ.get("OAUTH_CLIENT_ID") or "your-oauth-client-id"
    OAUTH_CLIENT_SECRET = os.environ.get("OAUTH_CLIENT_SECRET") or "your-oauth-client-secret"
    OAUTH_REDIRECT_URI =  "https://127.0.0.1:5000/auth/google/callback"
    # Google Fit Scopes (space-separated)
    GOOGLE_FIT_SCOPES = (
    "openid email profile "
    "https://www.googleapis.com/auth/fitness.activity.read "
    "https://www.googleapis.com/auth/fitness.heart_rate.read "
    "https://www.googleapis.com/auth/fitness.sleep.read"
)

    # Additional configuration variables can be added here...
