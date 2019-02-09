package com.infopulse.configuration;

import com.infopulse.controller.WebSocketController;
import com.infopulse.interceptor.WebSocketSecurityInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(getWebSocketController(), "/socket")
                .setAllowedOrigins("*")
                .addInterceptors(getWebSecurityInterceptor())
                .withSockJS();
    }

    @Bean
    public WebSocketSecurityInterceptor getWebSecurityInterceptor(){
        return new WebSocketSecurityInterceptor();
    }

    @Bean
    public  WebSocketController getWebSocketController(){
        return new  WebSocketController();
    }

}
