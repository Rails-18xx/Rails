package net.sf.rails.game.ai;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Map;

/**
 * The "Brain" of the AI.
 * Loads a pre-trained ONNX model and estimates the value of a game state.
 */
public class NeuralNetEvaluator {

    private final OrtEnvironment env;
    private final OrtSession session;

    public NeuralNetEvaluator(String modelPath) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        // Load the model (e.g., "rails_1835_v1.onnx")
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
    }

    /**
     * Evaluates a game state vector.
     * @param stateVector The flattened board state (from StateVectorBuilder)
     * @return The predicted final cash value (The "Score")
     */
    public float evaluate(double[] stateVector) {
        try {
            // 1. Convert double[] (Java) to FloatBuffer (ONNX)
            // PyTorch models usually expect Float32
            FloatBuffer buffer = FloatBuffer.allocate(stateVector.length);
            for (double v : stateVector) {
                buffer.put((float) v);
            }
            buffer.flip();

            // 2. Create Tensor [BatchSize=1, Features=N]
            long[] shape = new long[]{1, stateVector.length};
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape);

            // 3. Run Inference
            // "input" must match the name in the Python export script (input_names=['input'])
            OrtSession.Result result = session.run(Collections.singletonMap("input", inputTensor));

            // 4. Extract Output
            // Output is [[value]] (Batch x 1)
            float[][] output = (float[][]) result.get(0).getValue();
            
            // Cleanup to prevent memory leaks
            inputTensor.close();
            result.close();

            return output[0][0];

        } catch (OrtException e) {
            e.printStackTrace();
            return -1.0f; // Error score
        }
    }
    
    public void close() throws OrtException {
        if (session != null) session.close();
        if (env != null) env.close();
    }
}