from werkzeug.security import generate_password_hash, check_password_hash
from app.repositories import UserRepository, CalendarRepository

class UserService:
    """_summary_
    """
    def __init__(self, session=None):
        """
        Initialize the UserService with an optional SQLAlchemy session.
        If no session is provided, the service will use the default from UserRepository.
        """
        self.user_repo = UserRepository(session=session)
        self.calendar_repo = CalendarRepository()

    #--------------------------------
    # Updating user profile details
    #--------------------------------
    def update_user_profile(self, user_id: str, data: dict) -> tuple[dict, int]:
        """
        Update user profile details (excluding password).

        Args:
            user_id (int): The unique identifier for the user.
            data (dict): A dictionary containing fields to update (e.g., 'name', 'email', 'age').

        Returns:
            A tuple (response_dict, status_code).
        """
        user = self.user_repo.get_user_by_id(user_id)
        if not user:
            return {"message": "User not found"}, 404

        # Update only provided fields
        if "name" in data:
            user.name = data["name"]
        if "email" in data:
            user.email = data["email"]
        if "age" in data:
            user.age = data["age"]
        if "gender" in data:
            user.gender = ["gender"]

        self.user_repo.session.commit()
        return {"message": "User updated successfully", "user": user.to_dict()}, 200

    #--------------------------------
    # Changing user password
    #--------------------------------
    def change_password(self, user_id: str, old_password: str, new_password: str) -> tuple[dict, int]:
        """
        Change the user's password.

        Args:
            user_id (int): The unique identifier for the user.
            old_password (str): The user's current password.
            new_password (str): The new password to set.

        Returns:
            A tuple (response_dict, status_code).
        """
        user = self.user_repo.get_user_by_id(user_id)
        if not user:
            return {"message": "User not found"}, 404

        if not check_password_hash(user.password_hash, old_password):
            return {"message": "Incorrect old password"}, 401

        user.password_hash = generate_password_hash(new_password)
        self.user_repo.session.commit()
        return {"message": "Password updated successfully"}, 200

    #--------------------------------
    # Deleting user account
    #--------------------------------
    def delete_user(self, user_id: str, password: str) -> tuple[dict, int]:
        """
        Delete the user account after verifying password.
        """
        if not password:
            return {"message": "Password is required"}, 400

        user = self.user_repo.get_user_by_id(user_id)
        if not user:
            return {"message": "User not found"}, 404

        if not check_password_hash(user.password_hash, password):
            return {"message": "Incorrect password"}, 401

        user_deleted = self.user_repo.delete_user(user_id)
        if not user_deleted:
            return {"message": "Failed to delete user"}, 500

        calendar_deleted = self.calendar_repo.delete_calendar(user_id)
        if not calendar_deleted:
            return {"message": "User deleted, but calendar not found"}, 200  # partial success

        return {"message": "User and calendar deleted successfully"}, 200


# Example usage (this part is for testing purposes and should not be executed in production):
if __name__ == "__main__":
    # Example: Use a test user id (replace with actual test id)
    test_user_id = 1
    service = UserService()

    # Update user profile example
    update_response, status = service.update_user_profile(test_user_id, {"name": "John Doe", "email": "john@example.com", "age": 30})
    print(status, update_response)

    # Change password example
    cp_response, status = service.change_password(test_user_id, "old_password", "new_secure_password")
    print(status, cp_response)

    # Delete user example
    del_response, status = service.delete_user(test_user_id)
    print(status, del_response)
