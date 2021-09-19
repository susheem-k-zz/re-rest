package com.core.rerest.controller;

import com.core.rerest.service.StatefulRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping(path = "/test")
public class TestController {

    private final StatefulRestTemplate statefulRestTemplate;

    @Autowired
    public TestController(final StatefulRestTemplate statefulRestTemplate) {
        this.statefulRestTemplate = statefulRestTemplate;
    }

    @GetMapping(path = "/stateful-get")
    public ResponseEntity<String> testRestTemplate() throws ExecutionException, InterruptedException {
        return statefulRestTemplate.doGet(
                "http://localhost:5000/test-cache",
                null,
                null,
                String.class
        );
    }

}
