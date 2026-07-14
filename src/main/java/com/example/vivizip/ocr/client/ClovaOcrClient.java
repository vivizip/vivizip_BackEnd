package com.example.vivizip.ocr.client;

import com.example.vivizip.ocr.dto.ClovaOcrRequest;
import com.example.vivizip.ocr.dto.ClovaOcrResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClovaOcrClient {

    private final RestClient restClient;
    private final String invokeUrl;
    private final String secretKey;

    public ClovaOcrClient(
            RestClient restClient,
            @Value("${clova.ocr.invoke-url}") String invokeUrl,
            @Value("${clova.ocr.secret-key}") String secretKey
    ) {
        this.restClient = restClient;
        this.invokeUrl = invokeUrl;
        this.secretKey = secretKey;
    }

    public ClovaOcrResponse callOcr(ClovaOcrRequest request) {
        return restClient.post()
                .uri(invokeUrl)
                .header("X-OCR-SECRET", secretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ClovaOcrResponse.class);
    }
}
