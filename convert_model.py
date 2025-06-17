import tensorflow as tf
import os

def convert_to_tflite():
    print("Loading TensorFlow model...")
    try:
        # Load the TensorFlow model
        model = tf.keras.models.load_model('app/src/main/assets/fsrcnn_x2.pb')
        
        # Convert to TensorFlow Lite
        print("Converting to TensorFlow Lite...")
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float32]
        tflite_model = converter.convert()
        
        # Save the TensorFlow Lite model
        print("Saving TensorFlow Lite model...")
        with open('app/src/main/assets/fsrcnn_x2.tflite', 'wb') as f:
            f.write(tflite_model)
        
        print("Conversion completed successfully!")
    except Exception as e:
        print(f"Error during conversion: {str(e)}")
        raise

if __name__ == "__main__":
    convert_to_tflite() 