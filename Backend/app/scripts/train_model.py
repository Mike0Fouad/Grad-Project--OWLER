from app.repositories.ML_dataPipeline import MLDataPipeline
from app.scripts.generate_data import main as generate_data
import gc
import json
if __name__ == "__main__":
    # Generate data for 100 users
    
    for i in range(1, 2):
        x =0
        del x
        gc.collect()
        pipeline = MLDataPipeline()
        pipeline.train_from_json("app/data/user_data.json",True)
        del pipeline
        gc.collect()
