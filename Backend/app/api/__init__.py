from app.api import auth_routes, calendar_routes, user_routes


    
    
def create_blueprints():
    """
    Return a list of API blueprints.
    """
    return [
        auth_routes.bp,      # Assuming auth_routes.py defines a blueprint named "bp"
        calendar_routes.bp,  # Similarly for calendar routes
        user_routes.bp       # And user routes
    ]

 # Assuming you have a function to register CLI commands in calendar_routes.py