# at the VERY TOP of app/__init__.py, before any other imports
import flask.json
import json

# Re-create the old name so flask-mongoengine can import it:
flask.json.JSONEncoder = json.JSONEncoder

from flask import Flask
from .config import Config
from .extensions import init_extensions
from .api import create_blueprints  # A function to gather all your blueprints

def create_app():
    """Create and configure the Flask application.

    Returns:
        Flask: The configured Flask application instance.
    """
    app = Flask(__name__)
    app.config.from_object(Config)
    
    # Initialize all extensions (SQLAlchemy, JWT, PyMongo)
    init_extensions(app)
    
    # Register API blueprints (e.g., auth, calendar, user)
    blueprints = create_blueprints()
    for bp in blueprints:
        app.register_blueprint(bp)
    
    return app
