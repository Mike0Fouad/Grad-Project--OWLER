from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from app.services.user_service import UserService
from app.utils.validate import *
bp = Blueprint('user', __name__, url_prefix='/users')

# link: https://127.0.0.1:5000/users/profile
@bp.route('/profile', methods=['PUT'])
@jwt_required()
def update_profile():
    """
    Update user profile information
    """
    try:
        user_service = UserService()
        user_id = get_jwt_identity()
        print("success")
        data = request.get_json()
        valid= Validator.validate_profile(data)
        if not valid:
            return {'message': valid}, 400
        response, status_code = user_service.update_user_profile(user_id, data)
        return jsonify(response), status_code
    except Exception as e:
        return jsonify({"message": str(e)}), 500


# link: https://127.0.0.1:5000/users/change-password
@bp.route('/change-password', methods=['POST'])
@jwt_required()
def change_password():
    """
    Change user password
    """
    try:
        user_service = UserService()
        user_id = get_jwt_identity()
        data = request.get_json()
        valid = Validator.validate_password(data.get('newPassword'))
        
        valid = Validator.validate_password(data.get('oldPassword'))
        
        if not data or 'oldPassword' not in data or 'newPassword' not in data:
            return jsonify({"message": "Missing required fields"}), 400
            
        response, status_code = user_service.change_password(
            user_id=user_id,
            old_password=data['oldPassword'],
            new_password=data['newPassword']
        )
        return jsonify(response), status_code
    except KeyError:
        return jsonify({"message": "Invalid request format"}), 400
    except Exception as e:
        return jsonify({"message": str(e)}), 500

# link: https://127.0.0.1:5000/users/delete
@bp.route('/delete', methods=['DELETE'])
@jwt_required()
def delete_account():
    """
    Delete user account
    """
    try:
        user_service = UserService()
        #compares password with the one in the database
        data = request.get_json()
        if not data or 'password' not in data:
            return jsonify({"message": "Missing required fields"}), 400
        
        user_id = get_jwt_identity()
        response, status_code = user_service.delete_user(user_id, data['password'])
        return jsonify(response), status_code
    except Exception as e:
        return jsonify({"message": str(e)}), 500