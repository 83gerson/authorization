package com.example.authorization.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    @Autowired
    private RestTemplate restTemplate;

    public byte[] processAuthorization(byte[] byteArray) throws Exception {
        logger.info("Datos recibidos en Authorization: {}", new String(byteArray));

        String isoString = new String(byteArray);
        String numeroTarjeta = isoString.substring(20, 36);
        String montoTransaccion = isoString.substring(42, 54);
        String numeroReferenciaSeguimiento = isoString.substring(80, 92);
        String identificacionComercio = isoString.substring(98, 106);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();

        jsonNode.put("numeroTarjeta", numeroTarjeta);
        jsonNode.put("montoTransaccion", Integer.parseInt(montoTransaccion));
        jsonNode.put("numeroReferenciaSeguimiento", numeroReferenciaSeguimiento);
        jsonNode.put("identificacionComercio", identificacionComercio);
    
        
        String marcaTarjeta = identificarMarcaTarjeta(numeroTarjeta);
        jsonNode.put("marcaTarjeta", marcaTarjeta);

        String jsonString = jsonNode.toString();
        logger.info("JSON enviado a Apigateway2: {}", jsonString);

        String gateway2Url = "https://apigateway2.onrender.com/api/gateway2/receive";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonString, headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                gateway2Url,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        return byteArray;
    }

    public byte[] processJsonToIso(String json) throws Exception {
        logger.info("JSON recibido en Authorization para conversión a ISO: {}", json);
    
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);
    
        StringBuilder isoMessage = new StringBuilder();
        isoMessage.append("0210");
    
        String codigoProcesamiento = jsonNode.has("codigo_procesamiento") ? jsonNode.get("codigo_procesamiento").asText() : "";
        int montoTransaccion = jsonNode.has("monto") ? jsonNode.get("monto").asInt() : 0;
        String numeroSecuencia = jsonNode.has("num_secuencia") ? jsonNode.get("num_secuencia").asText() : "";
        String numeroReferenciaSeguimiento = jsonNode.has("num_seguimiento") ? jsonNode.get("num_seguimiento").asText() : "";
        String identificadorAutorizacion = jsonNode.has("identificador") ? jsonNode.get("identificador").asText() : "";
        String identificacionComercio = jsonNode.has("comercio") ? jsonNode.get("comercio").asText() : "";
    
        // Determina el valor correcto para codigoProcesamiento
        if ("aprobado".equalsIgnoreCase(codigoProcesamiento)) {
            codigoProcesamiento = "000000";
        } else if ("declinado".equalsIgnoreCase(codigoProcesamiento)) {
            codigoProcesamiento = "000051";
        }
    
        isoMessage.append(String.format("%-6s", codigoProcesamiento));
        isoMessage.append(String.format("%012d", montoTransaccion));
        isoMessage.append(String.format("%-6s", numeroSecuencia));
        isoMessage.append(String.format("%-12s", numeroReferenciaSeguimiento));
        isoMessage.append(String.format("%-6s", identificadorAutorizacion));
        isoMessage.append(String.format("%-8s", identificacionComercio));
    
        byte[] isoBytes = isoMessage.toString().getBytes();
    
        String gateway1Url = "https://apigateway1.onrender.com/api/gateway/receiveIso";
    
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(isoBytes, headers);
        ResponseEntity<byte[]> responseEntity = restTemplate.exchange(
                gateway1Url,
                HttpMethod.POST,
                requestEntity,
                byte[].class
        );
    
        logger.info("ISO enviado a Apigateway1: {}", new String(responseEntity.getBody()));
    
        return responseEntity.getBody();
    }

    private String identificarMarcaTarjeta(String numeroTarjeta) {
        if (numeroTarjeta.startsWith("4")) {
            return "VISA";
        } else if (numeroTarjeta.matches("^5[1-5].*") || numeroTarjeta.matches("^2(2[2-9]|[3-6][0-9]|7[0-1]|720).*")) {
            return "MASTERCARD";
        } else {
            return "DESCONOCIDA";
        }
    }

}