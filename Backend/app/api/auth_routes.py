# app/blueprints/auth.py
from urllib.parse import urlencode
from flask import Blueprint, request, redirect, current_app, url_for
from flask_jwt_extended import jwt_required
from app.services.auth_service import AuthService
from app.utils.validate import *
bp = Blueprint('auth', __name__, url_prefix='/auth')

# link: https://127.0.0.1:5000/auth/register
@bp.route('/register', methods=['POST'])
def register():
    """
    Register a new user with email/password
    """
    auth_service = AuthService()
    data = request.get_json()
    valid= Validator.validate_register(data)
    if not valid:
        return {'message': valid}, 400
    response, status_code = auth_service.register_user(data)
    return response, status_code

# link: https://127.0.0.1:5000/auth/login
@bp.route('/login', methods=['POST'])
def login():
    """
    Authenticate user with email/password
    """
    auth_service = AuthService()
    data = request.get_json()
    valid = Validator.validate_login(data)
    if  valid:
        return {'message': valid}, 400
    response, status_code = auth_service.login_user(data)
    return response, status_code



# link: https://127.0.0.1:5000/auth/google-signin
@bp.route("/google-signin", methods=["POST"])
def google_signin():
    """
    Sign in using Google ID token
    """
    auth_service = AuthService()
    
    data = request.json
    
    if not data or 'id_token' not in data:
        return {"msg": "Missing ID token"}, 400

    response, status_code = auth_service.handle_google_signin(data)
    return response, status_code
# link: http://127.0.0.1:5000/auth/refresh
@bp.route('/refresh', methods=['POST'])
@jwt_required(refresh=True)
def refresh():
    """
    Refresh access token using refresh token
    """
    auth_service = AuthService()
    response, status_code = auth_service.refresh_access_token()
    return response, status_code