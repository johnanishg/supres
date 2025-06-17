import os
import requests
import shutil

def download_file(url, filename):
    print(f"Downloading {filename}...")
    response = requests.get(url, stream=True)
    response.raise_for_status()
    
    with open(filename, 'wb') as f:
        for chunk in response.iter_content(chunk_size=8192):
            f.write(chunk)
    print(f"Downloaded {filename} successfully")

def main():
    # Create assets directory if it doesn't exist
    os.makedirs('app/src/main/assets', exist_ok=True)
    
    # Download the pre-trained FSRCNN x2 model in TFLite format
    model_url = "https://github.com/Saafke/FSRCNN_Tensorflow/raw/master/models/FSRCNN_x2.tflite"
    model_path = "app/src/main/assets/fsrcnn_x2.tflite"
    
    try:
        download_file(model_url, model_path)
        print(f"Model saved to {model_path}")
    except Exception as e:
        print(f"Error downloading model: {str(e)}")
        print("Please check your internet connection and try again.")

if __name__ == "__main__":
    main() 