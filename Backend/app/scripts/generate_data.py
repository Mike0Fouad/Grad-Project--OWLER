# generate_synthetic_data.py
"""
Synthetic data generation script using Faker.
Each user has 365 daily entries with timestamps, hourly data, sleep, Google Fit, tasks, schedule, and ML predictions.
"""
from faker import Faker
import random
import json
from datetime import datetime, timedelta
import math

fake = Faker()

def generate_daily_entry(base_date):
    """Generate one day's data for a user starting at base_date (midnight)."""
    # Collected and processed timestamps (in ISO format), ensuring chronological order
    collected_at = base_date + timedelta(hours=random.randint(6, 9))
    processed_at = collected_at + timedelta(hours=random.randint(1, 3))

    # Hourly activity metrics for 24 hours
    hourly_metrics = {}
    for hour in range(24):
        start = f"{hour:02d}:00"
        end = f"{(hour+1)%24:02d}:00"
        key = f"{start}-{end}"
        # Simulate steps (peaks during day, low at night)
        base = 100 + 200 * math.sin((hour+1)/24 * math.pi)  # base activity
        steps = max(0, int(random.gauss(base * 10, 50)))
        # Heart rate influenced by steps
        heart_rate = int(random.gauss(60 + steps * 0.02, 5))
        # Time features (cyclical encoding of hour)
        sin_hour = math.sin(2 * math.pi * hour / 24)
        cos_hour = math.cos(2 * math.pi * hour / 24)
        time_features = {"hour": hour, "hour_sin": sin_hour, "hour_cos": cos_hour}
        hourly_metrics[key] = {
            "steps": steps,
            "heart_rate": heart_rate,
            "time_features": time_features
        }
    
    # Sleep data with realistic proportions
    total_sleep = round(random.uniform(6, 9), 1)
    deep = round(total_sleep * random.uniform(0.2, 0.25), 1)
    rem = round(total_sleep * random.uniform(0.2, 0.25), 1)
    light = round(total_sleep - deep - rem, 1)
    sleep_data = {
        "total_hours": total_sleep,
        "deep_hours": deep,
        "rem_hours": rem,
        "light_hours": light,
        "awake_episodes": random.randint(0, 5)
    }

    # Google Fit-like summary data
    google_fit_data = {
        "hrv": random.randint(20, 100),  # heart rate variability (ms)
        "resting_heart_rate": random.randint(50, 70),
        "active_minutes": random.randint(30, 120),
        "distance": round(random.uniform(2, 10), 2),      # in kilometers
        "calories_burned": round(random.uniform(1800, 3000), 1)
    }

    # Task metrics per hour (aggregated scores)
    tasks_data = {}
    for hour in range(24):
        start = f"{hour:02d}:00"
        end = f"{(hour+1)%24:02d}:00"
        key = f"{start}-{end}"
        mental = random.uniform(0, 100)
        physical = random.uniform(0, 100)
        exhaustion = random.uniform(0, 100)
        tasks_data[key] = {
            "mental": round(mental, 1),
            "physical": round(physical, 1),
            "exhaustion": round(exhaustion, 1)
        }

    # Daily schedule performance indicators
    schedule_data = {
        "tasks_completed": random.randint(0, 10),
        "performance_score": round(random.uniform(0, 1), 2),
        "on_time_ratio": round(random.uniform(0.5, 1.0), 2)
    }

    # Compute next-day ML predictions correlated with today's activity
    avg_mental = sum(v["mental"] for v in tasks_data.values()) / 24
    avg_physical = sum(v["physical"] for v in tasks_data.values()) / 24
    predicted_CP = round(avg_mental * random.uniform(0.8, 1.2), 2)
    predicted_PE = round(avg_physical * random.uniform(0.8, 1.2), 2)
    ml_data = {"predicted_CP": predicted_CP, "predicted_PE": predicted_PE}

    return {
        "collected_at": collected_at.isoformat(),
        "processed_at": processed_at.isoformat(),
        "hourly_metrics": hourly_metrics,
        "sleep_data": sleep_data,
        "google_fit_data": google_fit_data,
        "tasks_data": tasks_data,
        "schedule_data": schedule_data,
        "ml_data": ml_data
    }

def generate_user_data(user_id, num_days=365):
    """Generate a user document with 365 days of synthetic data."""
    start_date = datetime(2025, 1, 1)  # fixed start date for simplicity
    user_record = {
        "user_id": user_id,
        "data": []
    }
    for day_offset in range(num_days):
        current_date = start_date + timedelta(days=day_offset)
        daily_entry = generate_daily_entry(current_date)
        user_record["data"].append(daily_entry)
    return user_record

def main():
    # Generate data for 100 users
    days = 300
    num_users = 300
    all_users = []
    for uid in range(1, num_users + 1):
        user_data = generate_user_data(uid, num_days=days)
        all_users.append(user_data)
    # Write out to JSON file
    with open("app/data/user_data.json", "w") as fout:
        json.dump(all_users, fout, indent=2)
    print(f"Generated synthetic data for {num_users} users in user_data.json")

if __name__ == "__main__":
    main()
