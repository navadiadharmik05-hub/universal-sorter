package org.example;

import java.util.List;

public interface AiBackend {
    boolean isAvailable();
    List<String> listModels();
    Classification classify(FileContext context, List<String> categories, double threshold) throws Exception;
    void warmUp() throws Exception;
}
