package com.example.authorization.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authorization.service.AuthorizationService;

@RestController
@RequestMapping("/api/authorization")
public class AuthorizationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationController.class);

    @Autowired
    private AuthorizationService authorizationService;

    @PostMapping(value = "/authorize", consumes = "application/octet-stream", produces = "application/octet-stream")
    public ResponseEntity<byte[]> authorize(@RequestBody byte[] byteArray) {
        try {
            byte[] responseBytes = authorizationService.processAuthorization(byteArray);
            return ResponseEntity.ok(responseBytes);
        } catch (Exception e) {
            logger.error("Error procesando los bytes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(byteArray);
        }
    }

    @PostMapping(value = "/receiveJson", consumes = "application/json", produces = "application/octet-stream")
    public ResponseEntity<byte[]> receiveJson(@RequestBody String json) {
        try {
            byte[] isoBytes = authorizationService.processJsonToIso(json);
            return ResponseEntity.ok(isoBytes);
        } catch (Exception e) {
            logger.error("Error procesando el JSON", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
