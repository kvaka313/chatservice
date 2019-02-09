package com.infopulse.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class WebSocketSecurityInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        final ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        final HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

        String token = httpServletRequest.getParameter("Authorization");

        Date expr = getExpireData(token);
        if(expr.before(new Date())){
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        String role = getRole(token);
        if(!"ROLE_USER".equals(role)){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        String login = getUserLogin(token);
        if(login == null){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put("login", login);

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {

    }

    private Date getExpireData(String token) throws IOException {
        String[] tokenParts = token.split("\\.");
        String body = new String(Base64.decodeBase64(tokenParts[1]), "UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> json = mapper.readValue(body, Map.class);
        Integer expr = (Integer)json.get("exp");
        long dataInMills = expr.intValue()*1000l;
        return new Date(dataInMills);
    }

    private String getRole(String token) throws IOException {
        String[] tokenParts = token.split("\\.");
        String body = new String(Base64.decodeBase64(tokenParts[1]), "UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> json = mapper.readValue(body, Map.class);
        Map<String, Object> resources = (Map<String, Object>)json.get("resource_access");
        Map<String, Object> service = (Map<String, Object>)resources.get("chatservice");
        List<String> roles = (List<String>)service.get("roles");
        return roles.get(0);
    }

    private String getUserLogin(String token) throws IOException {
        String[] tokenParts = token.split("\\.");
        String body = new String(Base64.decodeBase64(tokenParts[1]), "UTF-8");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> json = mapper.readValue(body, Map.class);
        String login = (String)json.get("preferred_username");
        return login;
    }
}
