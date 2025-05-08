import datetime
import pandas as pd
import json
import os
import numpy as np
import math
import joblib
from sklearn.metrics import r2_score
from sklearn.pipeline import Pipeline
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import StandardScaler
from sklearn.multioutput import MultiOutputRegressor
from xgboost import XGBRegressor
from sklearn.model_selection import train_test_split
from sklearn.model_selection import RandomizedSearchCV
from scipy.stats import uniform, randint
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error
import logging
from app.repositories.calendar_repository import CalendarRepository
logger = logging.getLogger(__name__)

class MLDataPipeline:
    
    def __init__(
        self,
        model_path="./model_data/global_model.joblib",
        private_model_path="./model_data/private_model_{user_id}.joblib",
        file_data_dir: str = None
    ):
        """
        file_data_dir: Optional directory containing JSON files
                       named by user or date, e.g. user_123.json or data_2025-01-01.json
        """
        self.calendar_repo = CalendarRepository()
        self.model_path = model_path
        self.private_model_path = private_model_path
        self.file_data_dir = file_data_dir
        
        self.feature_columns = [
            'steps', 'heart_rate', 'mental_load', 'physical_load',
            'exhaustion', 'total_sleep', 'deep_sleep', 'rem_sleep',
            'light_sleep', 'hrv', 'sin_time', 'cos_time'
        ]

        # Build preprocessing + model pipeline
        self.preprocessor = ColumnTransformer(
            transformers=[
                ('num', Pipeline(steps=[
                    ('imputer', SimpleImputer(strategy='median')),
                    ('scaler', StandardScaler())
                ]), self.feature_columns)
            ]
        )
        self.pipeline = Pipeline([
            ('preprocessor', self.preprocessor),
            ('regressor', MultiOutputRegressor(
                XGBRegressor(objective='reg:squarederror', n_estimators=100, random_state=42)
            ))
        ])
    
    def _parse_json_user(self, user_obj: dict):
        """Parse one JSON user's data (list of days)."""
        days = user_obj.get('data', [])
        X, y = [], []
        for i in range(len(days)-1):
            today = days[i]
            tomorrow = days[i+1]
            try:
                feat = self._extract_daily_features(today)
                # assume tomorrow has ml_data dict
                md = tomorrow.get('ml_data', {})
                targets = md.get('predicted_CP'), md.get('predicted_PE')
                if None in (feat, *targets):
                    continue
                X.append(feat)
                y.append(list(targets))
            except Exception as e:
                logger.warning(f"JSON parse error on user: {e}")
        if not X:
            return None, None
        return np.array(X), np.array(y)

    def tune_hyperparameters(self, X, y, n_iter=20, cv=3, scoring='neg_mean_absolute_error'):
        """
        Perform randomized search over XGBRegressor hyperparameters.
        Returns the best estimator and CV results.
        """
        param_dist = {
            'regressor__estimator__n_estimators': randint(50, 300),
            'regressor__estimator__max_depth': randint(3, 10),
            'regressor__estimator__learning_rate': uniform(0.01, 0.3),
            'regressor__estimator__subsample': uniform(0.5, 0.5),
            'regressor__estimator__colsample_bytree': uniform(0.5, 0.5)
        }
        search = RandomizedSearchCV(
            self.pipeline,
            param_distributions=param_dist,
            n_iter=n_iter,
            cv=cv,
            scoring=scoring,
            random_state=42,
            verbose=1,
            n_jobs=-1
        )
        X_df = pd.DataFrame(X, columns=self.feature_columns)
        search.fit(X_df, y)
        logger.info(f"Best params: {search.best_params_}")
        logger.info(f"Best CV score: {search.best_score_}")
        # update pipeline
        self.pipeline = search.best_estimator_
        return search
        
    def load_data_all_users(self):
        """
        Combines database and optional file-based data.
        """
        X_list, y_list = [], []

        # 1) Load from DB
        for uid in self.calendar_repo.get_all_users_id():
            X, y = self.load_and_validate_data(uid)
            if X is not None and len(X):
                X_list.append(X)
                y_list.append(y)

        # 2) Load from JSON files, if directory provided
        if self.file_data_dir and os.path.isdir(self.file_data_dir):
            for fname in os.listdir(self.file_data_dir):
                if not fname.endswith('.json'):
                    continue
                full = os.path.join(self.file_data_dir, fname)
                with open(full, 'r') as f:
                    try:
                        user_obj = json.load(f)
                        Xf, yf = self._parse_json_user(user_obj)
                        if Xf is not None and len(Xf):
                            X_list.append(Xf)
                            y_list.append(yf)
                    except Exception as e:
                        logger.error(f"Failed loading {fname}: {e}")
        
        if not X_list:
            return None, None
        return np.vstack(X_list), np.vstack(y_list)
    
    def load_and_validate_data(self, user_id):
        """Load and validate training data from MongoDB"""
        try:
            all_data = self.calendar_repo.get_all_UserData(user_id)
            if len(all_data) < 2:
                logger.warning("Insufficient data for training")
                return None, None

            X, y = [], []
            
            # Pair consecutive days: day N -> predicts day N+1
            for i in range(len(all_data)-1):
                current_day = all_data[i]
                next_day = all_data[i+1]

                if not (current_day.GoogleFitData and next_day.MLData):
                    continue

                # Validate time slots
                current_slots = {m.hour_range for m in current_day.GoogleFitData.hourly_metrics}
                next_slots = {m.time_slot for m in next_day.MLData}
                
                if current_slots != next_slots:
                    logger.warning(f"Slot mismatch between days {i} and {i+1}")
                    continue

                # Build features/targets
                for ml_entry in next_day.MLData:
                    try:
                        features = self._extract_features(current_day, ml_entry.time_slot)
                        targets = [ml_entry.predicted_CP, ml_entry.predicted_PE]
                        
                        if None in features or None in targets:
                            continue
                            
                        X.append(features)
                        y.append(targets)
                    except Exception as e:
                        logger.error(f"Error processing entry: {str(e)}")

            return np.array(X), np.array(y)
            
        except Exception as e:
            logger.error(f"Data loading failed: {str(e)}")
            return None, None

    def _extract_features(self, day, time_slot):
        """Extract features for a specific time slot"""
        # Time features
        hour = int(time_slot.split('-')[0].split(':')[0])
        time_features = [
            math.sin(2 * math.pi * hour / 24),
            math.cos(2 * math.pi * hour / 24)
        ]

        # Health metrics
        metric = next(
            (m for m in day.GoogleFitData.hourly_metrics 
             if m.hour_range == time_slot), None
        )
        health_features = [
            metric.steps if metric else 0,
            metric.heart_rate if metric else 0,
            day.GoogleFitData.hrv
        ]

        # Task metrics
        task_data = day.AggregatedTaskData.slots.get(time_slot, {})
        task_features = [
            task_data.get('avg_mental', 0),
            task_data.get('avg_physical', 0),
            task_data.get('avg_exhaustion', 0)
        ]

        # Sleep features
        sleep_features = [
            day.GoogleFitData.sleep.total_hours,
            day.GoogleFitData.sleep.deep_hours,
            day.GoogleFitData.sleep.rem_hours,
            day.GoogleFitData.sleep.light_hours
        ]

        return health_features + task_features + sleep_features + time_features

    def train(self, user_id, optimize=False):
        """Full training workflow"""
        X, y = self.load_and_validate_data(user_id)
        if X is None or len(X) == 0:
            return False

        # Train/Test split maintaining temporal order
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, shuffle=False
        )

        # Optionally optimize hyperparameters first
        
        X_train_df = pd.DataFrame(X_train, columns=self.feature_columns)
        X_test_df = pd.DataFrame(X_test, columns=self.feature_columns)
        if optimize:
            self.tune_hyperparameters(X_train_df, y_train)
        else:
            self.pipeline.fit(X_train_df, y_train)
        
        # Evaluate
        test_pred = self.pipeline.predict(X_test_df)
        mae_cp = mean_absolute_error(y_test[:,0], test_pred[:,0])
        mae_pe = mean_absolute_error(y_test[:,1], test_pred[:,1])
        
        logger.info(f"Model trained - Test MAE: CP={mae_cp:.3f}, PE={mae_pe:.3f}")
        
        # Save model
        joblib.dump(self.pipeline, self.model_path)
        return True
    
    def train_global(self):
        """Train a global model on all users."""
        X, y = self.load_data_all_users()
        if X is None:
            logger.error("No data for global training")
            return False
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, shuffle=True)
        self.global_pipeline.fit(X_train, y_train)
        # evaluate
        preds = self.global_pipeline.predict(X_test)
        logger.info(f"Global model MAE: {mean_absolute_error(y_test, preds):.3f}")
        joblib.dump(self.global_pipeline, self.global_model_path)
        return True
    
    def train_private(self, user_id):
        """Train private residual model for a specific user."""
        X, y = self.load_and_validate_data(user_id)
        if X is None:
            logger.error(f"No data for user {user_id}")
            return False
        # load global model
        self.global_pipeline = joblib.load(self.global_model_path)
        # compute residuals
        global_pred = self.global_pipeline.predict(X)
        residuals = y - global_pred
        # split
        X_train, X_test, r_train, r_test = train_test_split(X, residuals, test_size=0.2, shuffle=False)
        # fit private
        self.private_pipeline.fit(X_train, r_train)
        # evaluate combined
        combined = self.global_pipeline.predict(X_test) + self.private_pipeline.predict(X_test)
        mae = mean_absolute_error(r_test + self.global_pipeline.predict(X_test), combined)
        logger.info(f"User {user_id} combined MAE: {mae:.3f}")
        # save
        pm_path = self.private_model_path.format(user_id=user_id)
        joblib.dump(self.private_pipeline, pm_path)
        return True

    def predict_next_day(self, current_day):
        """Generate predictions for all time slots of next day"""
        try:
            if not self.pipeline:
                self.pipeline = joblib.load(self.model_path)

            predictions = []
            for hour in range(24):
                time_slot = f"{hour:02d}:00-{(hour+1):02d}:00"
                day= datetime.strptime(current_day, "%Y-%m-%d")- datetime.timedelta(days=1) 
                features = self._extract_features(day, time_slot)
                pred = self.pipeline.predict([features])[0]
                
                # Ensure valid predictions
                predictions.append({
                    'time_slot': time_slot,
                    'CP': np.clip(pred[0], 0, 1),
                    'PE': np.clip(pred[1], 0, 1)
                })

            return predictions
        except Exception as e:
            logger.error(f"Prediction failed: {str(e)}")
            return []
        
    def _extract_daily_features(self, day: dict) -> list:
        """
        Flatten one day's JSON record into a 1D feature vector
        matching self.feature_columns:
          ['steps','heart_rate','mental_load','physical_load',
           'exhaustion','total_sleep','deep_sleep','rem_sleep',
           'light_sleep','hrv','sin_time','cos_time']
        """
        # 1) Check that day has the keys we need
        required = ["hourly_metrics","google_fit_data","tasks_data","sleep_data","collected_at"]
        for key in required:
            if key not in day:
                raise KeyError(f"Missing key '{key}' in day record")

        # 2) Health metrics: sum steps, avg heart rate, HRV
        hourly = day["hourly_metrics"]
        # Make sure hourly is a dict of dicts
        if not isinstance(hourly, dict):
            raise TypeError(f"hourly_metrics must be a dict, got {type(hourly)}")
        total_steps = sum(h["steps"] for h in hourly.values())
        avg_hr = sum(h["heart_rate"] for h in hourly.values()) / len(hourly)
        hrv = day["google_fit_data"]["hrv"]

        # 3) Task metrics: average over 24 slots
        tasks = day["tasks_data"]
        avg_mental = sum(t["mental"] for t in tasks.values()) / len(tasks)
        avg_physical = sum(t["physical"] for t in tasks.values()) / len(tasks)
        avg_exhaustion = sum(t["exhaustion"] for t in tasks.values()) / len(tasks)

        # 4) Sleep metrics
        sleep = day["sleep_data"]
        total_sleep = sleep["total_hours"]
        deep = sleep["deep_hours"]
        rem = sleep["rem_hours"]
        light = sleep["light_hours"]

        # 5) Time features from the collection timestamp
        #    e.g. "2025-01-01T08:00:00"
        coll = day["collected_at"]
        if isinstance(coll, str):
            try:
                dt = datetime.fromisoformat(coll)
            except AttributeError:
                from dateutil.parser import parse
                dt = parse(coll)
        elif isinstance(coll, datetime):
            dt = coll
        else:
            raise TypeError(f"collected_at must be str or datetime, got {type(coll)}")
        hour = dt.hour
        sin_t = math.sin(2 * math.pi * hour / 24)
        cos_t = math.cos(2 * math.pi * hour / 24)

        # 6) Build feature list in correct order
        return [
            total_steps,
            avg_hr,
            avg_mental,
            avg_physical,
            avg_exhaustion,
            total_sleep,
            deep,
            rem,
            light,
            hrv,
            sin_t,
            cos_t
        ]
     
    def train_from_json(self, json_path, optimize=False):
        """
        Load synthetic training data from a JSON file and train a global regression model.
        The JSON should contain 365 days of data for 100 users. Each day's data includes
        the 24-hour 'predicted_CP' and 'predicted_PE' values (used as the next-day target).
        """
        # Load JSON data
        with open(json_path, 'r') as f:
            users = json.load(f)

        
        X_list = []
        y_list = []

       
        for user in users:
            days = user["data"]  
            
            for i in range(len(days) - 1):
                today = days[i]
                

                features = self._extract_daily_features(today)
                X_list.append(features)

                # Build target: next day's predicted_CP and predicted_PE (24 values each)
                md= today["ml_data"]
                y_list.append([md["predicted_CP"], md["predicted_PE"]])
                
    

        # Convert lists to arrays/DataFrames
        X = np.array(X_list)  # shape (N_samples, n_features)
        # Alternatively, if feature order matters, convert via DataFrame:
        # import pandas as pd
        # X = pd.DataFrame(X_list)  # columns as feature keys
        y = np.array(y_list)  # shape (N_samples, 48) for multioutput

        # (Optional) Any additional preprocessing can be applied here
        # e.g., scaling or encoding if needed

        # Split into train and test sets
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42
        )
        X_train_df = pd.DataFrame(X_train, columns=self.feature_columns)
        X_test_df  = pd.DataFrame(X_test,  columns=self.feature_columns)

        # Optionally tune hyperparameters on this JSON data
        if optimize:
            self.tune_hyperparameters(X_train_df, y_train)
        else:
            self.pipeline.fit(X_train_df, y_train)

        # Train the model on training data
        

        # Predict on test set
        y_pred = self.pipeline.predict(X_test_df)

        # Compute evaluation metrics
        mae = mean_absolute_error(y_test, y_pred)
        r2 = r2_score(y_test, y_pred)

        # Print the scores clearly
        print(f"Mean Absolute Error (MAE): {mae:.4f}")
        print(f"R^2 Score: {r2:.4f}")

        # Save the trained model to the specified path
        
        os.makedirs(os.path.dirname(self.model_path),exist_ok=True)
        joblib.dump(self.pipeline, self.model_path) 
    