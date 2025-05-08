from datetime import datetime, timedelta
import logging
import app.repositories.ML_dataPipeline as MLDataPipelineModule
from app.repositories import UserRepository, CalendarRepository
logger = logging.getLogger(__name__)

class MLService:
    def __init__(self):
        self.calendar_repo = CalendarRepository()
        self.user_repo = UserRepository()
        # instantiate pipeline module (global + private support)
        self.pipeline = MLDataPipelineModule.MLDataPipeline()

    def daily_retrain_private(self, user_id):
        """Retrain only the private residual model for one user"""
        try:
            # ensure global model exists
            if not self.pipeline.train_global():
                logger.warning("Global model training failed; skipping private training")
                return False
            # train private model
            if self.pipeline.train_private(user_id):
                logger.info(f"Private retraining completed for {user_id}")
                return True
            else:
                logger.warning(f"No private data for user {user_id}")
                return False
        except Exception as e:
            logger.error(f"Private retraining failed for {user_id}: {e}")
            return False

    def daily_retrain_global(self):
        """Retrain the global model on all users"""
        try:
            if self.pipeline.train_global():
                logger.info("Global retraining completed successfully")
                return True
            else:
                logger.warning("No data for global retraining")
                return False
        except Exception as e:
            logger.error(f"Global retraining failed: {e}")
            return False

    def generate_predictions(self, user_id):
        """Generate and store next day's predictions using global+private models"""
        try:
            yesterday = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d")
            current_day = self.calendar_repo.get_UserData(user_id, yesterday)
            if not current_day or not getattr(current_day, 'GoogleFitData', None):
                logger.error(f"No data for user {user_id} on {yesterday}")
                return False

            # predict using both global and private
            predictions = self.pipeline.predict_next_day(current_day, user_id=user_id)
            if not predictions:
                return False

            today = datetime.now().strftime("%Y-%m-%d")
            success = self.calendar_repo.update_ml_predictions(user_id, today, predictions)
            if success:
                logger.info(f"Predictions stored for {user_id} on {today}")
                return True
            return False
        except Exception as e:
            logger.error(f"Prediction generation failed for {user_id}: {e}")
            return False

    def full_cycle_for_user(self, user_id):
        """Perform full cycle: global+private retrain then predict for a specific user"""
        # Retrain global model every time
        self.daily_retrain_global()
        # Retrain private for this user
        self.daily_retrain_private(user_id)
        # Generate predictions
        return self.generate_predictions(user_id)

    def full_cycle_global_and_all(self):
        """Perform global retrain then private retrain for all users and predictions"""
        results = {}
        if not self.daily_retrain_global():
            return results
        user_ids = self.user_repo.get_all_users_id()
        for uid in user_ids:
            self.pipeline.train_private(uid)
            results[uid] = self.generate_predictions(uid)
        return results
