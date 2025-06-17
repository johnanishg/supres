import tensorflow as tf
import numpy as np

def create_fsrcnn_model():
    print("Creating FSRCNN model...")
    try:
        # Create a simple FSRCNN model
        model = tf.keras.Sequential([
            # Feature extraction
            tf.keras.layers.Conv2D(32, (3, 3), padding='same', activation='relu', input_shape=(None, None, 3)),
            # Non-linear mapping
            tf.keras.layers.Conv2D(32, (3, 3), padding='same', activation='relu'),
            # Deconvolution
            tf.keras.layers.Conv2DTranspose(3, (3, 3), strides=(2, 2), padding='same', activation='linear')
        ])
        
        # Compile the model
        model.compile(optimizer='adam', loss='mse')
        
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
        
        print("Model created and saved successfully!")
    except Exception as e:
        print(f"Error creating model: {str(e)}")
        raise

if __name__ == "__main__":
    create_fsrcnn_model() 