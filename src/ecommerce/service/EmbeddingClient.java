package ecommerce.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class EmbeddingClient {

    public static float[] getEmbedding(String query) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "C:/Users/Dell/AppData/Local/Programs/Python/Python310/python.exe",
                    "embed_query.py",
                    query
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            if (output.toString().contains("Traceback")) {
                System.err.println("❌ Python script error:\n" + output);
                return new float[0];
            }

            String[] parts = output.toString().trim().replace("[", "").replace("]", "").split(",");
            float[] vector = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                vector[i] = Float.parseFloat(parts[i].trim());
            }

            return vector;
        } catch (Exception e) {
            e.printStackTrace();
            return new float[0];
        }
    }
}
