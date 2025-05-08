from flask_migrate import Migrate
from flask_sqlalchemy import SQLAlchemy
from flask_jwt_extended import JWTManager
from flask_mongoengine import MongoEngine

# Initialize SQLAlchemy for MySQL
mysql = SQLAlchemy()
migrate = Migrate()

# Initialize JWTManager for handling JWT tokens
jwt = JWTManager()

# Initialize MongoEngine for MongoDB
Mongo = MongoEngine()

# Initialize extensions with the app configuration
def init_extensions(app):
    """Initialize extensions with the Flask app.

    Args:
        app (Flask): The Flask application instance.
    """
    # Compatibility shim for Flask ≥ 2.2 + flask-mongoengine
    if not hasattr(app, 'json_encoder'):
        # Point json_encoder to the provider class itself
        app.json_encoder = app.json.__class__

    # Configure SQLAlchemy with the app configuration
    mysql.init_app(app)
    migrate.init_app(app, mysql)

    # Configure JWTManager with the app configuration
    jwt.init_app(app)

    # Configure MongoEngine with the app configuration
    Mongo.init_app(app)
