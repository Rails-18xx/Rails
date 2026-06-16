package net.sf.rails.game;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

// upload will go to https://docs.google.com/spreadsheets/d/1lXyCBjjLLzfeajpdS_PEqI3z4IGDIqj5gRAtiPpyqck/edit?pli=1&gid=0#gid=0

public class ResultUploader {
    private static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbybDcQ3X6QvuDTB0JiSDaIpM-sfmSfRCwEVE9KLSYFp-x-9XpfOSWSLVn8MGdM9CUCt/exec";

    public static void uploadGameResult(String gameName, List<Player> players, List<Integer> scores) {
        String playersJson = players.stream().map(p -> "\"" + p.getFullName() + "\"").collect(Collectors.joining(",", "[", "]"));
        String scoresJson = scores.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
        String jsonBody = String.format("{\"gameName\":\"%s\", \"players\":%s, \"scores\":%s}", gameName, playersJson,
                scoresJson);

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SCRIPT_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> System.out.println("Server Response: " + response))
                .exceptionally(ex -> {
                    System.err.println("Upload failed: " + ex.getMessage());
                    return null;
                });
    }
}