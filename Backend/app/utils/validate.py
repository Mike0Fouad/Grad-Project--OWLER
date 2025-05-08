import re

class Validator:
    EMAIL_REGEX = re.compile(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$')
    PASSWORD_MIN_LENGTH = 6

    @staticmethod
    def validate_name(name):
        if not name or not name.strip():
            return "Name cannot be empty."
        if not name.replace(" ", "").isalpha():
            return "Name must contain only letters and spaces."
        return False

    @staticmethod
    def validate_age(age):
        if age is None:
            return "Age cannot be empty."
        try:
            if  isinstance(age, str):
                age = int(age)
        except (ValueError, TypeError):
            return "Age must be a valid integer."
        if age < 0 or age > 120:
            return "Age must be between 0 and 120."
        return False

    @staticmethod
    def validate_gender(gender):
        if gender not in ('m', 'f'):
            return "Gender must be 'm' or 'f'."
        return False

    @staticmethod
    def validate_email(email):
        if not Validator.EMAIL_REGEX.match(email):
            return "Invalid email format."
        return False

    @staticmethod
    def validate_password(password):
        if not password or len(password) < Validator.PASSWORD_MIN_LENGTH:
            return f"Password must be at least {Validator.PASSWORD_MIN_LENGTH} characters long."
        return False

    @staticmethod
    def validate_register(data):
        for field, validator in [
            ("name", Validator.validate_name),
            ("age", Validator.validate_age),
            ("gender", Validator.validate_gender),
            ("email", Validator.validate_email),
            ("password", Validator.validate_password)
        ]:
            msg = validator(data.get(field))
            if msg:
                return msg
        return False

    @staticmethod
    def validate_login(data):
        for field, validator in [
            ("email", Validator.validate_email),
            ("password", Validator.validate_password)
        ]:
            msg = validator(data.get(field))
            if msg:
                return msg
        return False

    @staticmethod
    def validate_profile(data):
        for field, validator in [
            ("name", Validator.validate_name),
            ("age", Validator.validate_age),
            ("gender", Validator.validate_gender),
            ("email", Validator.validate_email)
        ]:
            msg = validator(data.get(field))
            if msg:
                return msg
        return False

    @staticmethod
    def validate_day(day):
        if not isinstance(day, int) or day < 1 or day > 31:
            return "Day must be an integer between 1 and 31."
        return False
