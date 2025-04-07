package com.ecsimsw.gateway.controller;

import com.ecsimsw.gateway.domain.BlockedUser;
import com.ecsimsw.gateway.domain.BURepo;
import com.ecsimsw.gateway.service.RouteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ecsimsw.common.config.LogConfig.TRACE_ID;

@Slf4j
@RequiredArgsConstructor
@RestController
public class RouteController {

    private final WebClient webClient;
    private final RouteService routeService;
    private final BURepo BURepo;

    @GetMapping("/api/hi")
    public ResponseEntity<Void> hi() {
        BURepo.save(new BlockedUser("sdfsdf"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/bye")
    public ResponseEntity<String> bye() {
        var hi = BURepo.findById("sdfsdf").orElseThrow();
        return ResponseEntity.ok(hi.toString());
    }

    @RequestMapping("/api/{service}/**")
    public Mono<ResponseEntity<String>> routeRequest(
        @PathVariable String service,
        @RequestBody(required = false) Optional<Object> requestBody,
        HttpServletRequest request,
        HttpMethod method
    ) {
        var endPoint = routeService.getEndPoint(service);
        var url = "http://" + endPoint.getHostName() + ":" + endPoint.getPort() + request.getRequestURI();
        var queryString = buildQueryString(request);
        if (!queryString.isBlank()) {
            url += "?" + queryString;
        }
        var headers = headers(request);
        return send(method, url, headers, requestBody);
    }

    private Mono<ResponseEntity<String>> send(HttpMethod method, String url, HashMap<String, String> headers, Optional<Object> requestBody) {
        log.info("Request url : {}\n" +
                "Request headers : {}\n" +
                "Request body : {}",
            url, headers, requestBody
        );
        return webClient
            .method(method)
            .uri(url)
            .headers(httpHeaders -> headers.forEach(httpHeaders::set))
            .bodyValue(requestBody)
            .retrieve()
            .toEntity(String.class)
            .onErrorResume(WebClientResponseException.class, exception -> {
                    var responseHeader = new HttpHeaders();
                    responseHeader.setContentType(MediaType.APPLICATION_JSON);
                    responseHeader.setContentLength(exception.getResponseBodyAsString().getBytes(StandardCharsets.UTF_8).length);
                    responseHeader.set(TRACE_ID, exception.getHeaders().getFirst(TRACE_ID));
                    return Mono.just(ResponseEntity
                        .status(exception.getStatusCode())
                        .headers(responseHeader)
                        .body(exception.getResponseBodyAsString()));
                }
            );
    }

    private HashMap<String, String> headers(HttpServletRequest request) {
        var headers = new HashMap<String, String>();
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            var headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    private String buildQueryString(HttpServletRequest request) {
        return request.getParameterMap().entrySet().stream()
            .map(entry -> Arrays.stream(entry.getValue())
                .map(value -> encodeParam(entry.getKey()) + "=" + encodeParam(value))
                .collect(Collectors.joining("&"))).collect(Collectors.joining("&"));
    }

    private String encodeParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
