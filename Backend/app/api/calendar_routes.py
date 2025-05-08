import click
from flask import Blueprint, request, redirect, current_app

from flask_jwt_extended import jwt_required, get_jwt_identity
from app.models.mongodb.schedule import Schedule, Task
from app.models.mongodb.user_data import AggregatedTaskData, GoogleFitData, MLData, ScheduleData, UserData
from app.services import CalendarService, MLService
from datetime import datetime
from app.models.mongodb import Day  # Import the Day class from the appropriate module

bp = Blueprint('calendar', __name__, url_prefix='/calendar')






## automated daily retraining

#backup route
# link: https://127.0.0.1:5000/calendar/upload-days
@bp.route('/upload-days', methods=['POST'])
@jwt_required()
def upload_days():
    calendar_service = CalendarService()
    user_id = get_jwt_identity()
    if not user_id:
        return {"error": "User ID is required"}, 400

    days = request.json.get('days')
      
    

    # Ensure required keys in schedule are initialized
    for key in ['start', 'end']:
        if key not in days['schedule'] or days['schedule'][key] is None:
            days['schedule'][key] = ''
    for key in ['dailyScore', 'exhaustion', 'done']:
        if key not in days['schedule'] or days['schedule'][key] is None:
            days['schedule'][key] = 1
    
    
   
    day = Day(
        date=days['date'],
        schedule=Schedule(
            start=days['schedule']['start'],
            end=days['schedule']['end'],
            done=days['schedule']['done'],
            exhaustion=days['schedule']['exhaustion'],
            daily_score=days['schedule']['dailyScore'],
            tasks = [Task(**task) for task in days['schedule'].get('tasks', [])]
        ),
        UserData=UserData(
            GoogleFitData=GoogleFitData(),
            ScheduleData=ScheduleData(),
            AggregatedTaskData=AggregatedTaskData(),
            MLData=[]
        )
    )
     
    
    operation = calendar_service.add_or_update_day_schedule(user_id,day)
    if not operation:
        return {"error": "Failed to sync data"}, 500

    return {"message": "Data synced successfully"}, 200



# link: https://127.0.0.1:5000/calendar/get-user-data
@bp.route('/get-user-data', methods=['GET'])
@jwt_required()
def get_user_data():
    """Get user data for a specific day"""
    calendar_service = CalendarService()
    ml_service = MLService()
    user_id = get_jwt_identity()
    if not user_id:
        return {"error": "User ID is required"}, 400
    
    date_str = request.args.get('date_str') # Removed args.json
    if not date_str:
        return {"error": "Date is required"}, 400
    
    # Get user data
    
    
    ml_service.full_cycle_for_user(user_id)
    user_data = calendar_service.get_user_data_for_day(user_id, date_str)
   
    UserData = UserData(
        GoogleFitData=GoogleFitData(),
        ScheduleData=ScheduleData(),
        AggregatedTaskData=AggregatedTaskData(),
        MLData=[]
    )
    
        
    return user_data, 200

# link: https://127.0.0.1:5000/calendar/get-days
@bp.route('/get-days', methods=['GET'])
@jwt_required()
def get_days():
    """Get all days for a specific user"""
    calendar_service = CalendarService()
    user_id = get_jwt_identity()
    if not user_id:
        return {"error": "User ID is required"}, 400
    
    date = request.args.get('date') 
    if not date:
        return {"error": "Date is required"}, 400
    
    days = calendar_service.get_day(user_id, date_str=date)  # Ensure correct attribute usage
    if not days:
        return {"error": "No data found"}, 404
    
    
    return days, 200


    user_id = get_jwt_identity()
    if not user_id:
        return {"error": "User ID is required"}, 400
    data    = request.get_json()
    date = request.args.get("date")
    data = data.get("data")
    
    # Validate structure:
    if not data or "hourly_metrics" not in data or "sleep" not in data:
        return {"error": "Malformed payload"}, 400

    # Persist via your CalendarService:
    success = CalendarService().update_google_fit_data(user_id, data)
    if not success:
        return {"error": "Storage failed"}, 500
    ML = MLService().generate_predictions(user_id)
    if not ML:
        return {"error": "ML prediction failed"}, 500
    # Return success response:
    return {"status": "ok"}, 200