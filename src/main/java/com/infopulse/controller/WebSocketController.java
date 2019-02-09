package com.infopulse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infopulse.dto.ReceiveMessage;
import com.infopulse.dto.SendMessage;
import com.infopulse.service.controllerservices.BanControllerService;
import com.infopulse.service.controllerservices.WebSocketServiceController;
import com.infopulse.validation.ReceiveMessageGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketController extends TextWebSocketHandler {

    @Autowired
    private WebSocketServiceController webSocketService;

    @Autowired
    BanControllerService banControllerService;

    @Autowired
    Validator validator;

    private Map<String, WebSocketSession> activeUsers = new ConcurrentHashMap<>();

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {
        if(banControllerService.verifyForBan((String)session.getAttributes().get("login"))){
            SendMessage sendMessage = createSendMessage("system", "You are banned", "PRIVATE");
            session.sendMessage(new TextMessage(mapper.writeValueAsString(sendMessage)));

            return;
        }
        activeUsers.put((String)session.getAttributes().get("login"), session);
        List<SendMessage> messageList = webSocketService.getAllMessage((String)session.getAttributes().get("login"));
        messageList.stream()
                   .peek(m -> sendPrivateMessage((String)session.getAttributes().get("login"), m)).count();

        webSocketService.deleteAllPrivateMessages((String)session.getAttributes().get("login"));
        sendAllChangeActiveList();
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws IOException {
        if(banControllerService.verifyForBan((String)session.getAttributes().get("login"))){
            session.close();
            return;
        }
         String jsonString = message.getPayload();
        ReceiveMessage receiveMessage = mapper.readValue(jsonString, ReceiveMessage.class);
        Set<ConstraintViolation<ReceiveMessage>> violations = validator.validate(receiveMessage, ReceiveMessageGroup.class);

        if(!violations.isEmpty()){
            String errorMessage = getErrorMessage(violations);
            SendMessage sendMessage = createSendMessage((String)session.getAttributes().get("login"), errorMessage, "ERROR");
            sendPrivateMessage((String)session.getAttributes().get("login"), sendMessage);
            return;
        }

        switch (receiveMessage.getType()){

            case "BROADCAST":{
                SendMessage sendMessage = createSendMessage((String)session.getAttributes().get("login"), receiveMessage.getMessage(), "BROADCAST");
                sendAll(sendMessage);
                webSocketService.saveBroadcastMessage(sendMessage);
                break;

            }
            case "PRIVATE":{
                SendMessage sendMessage = createSendMessage((String)session.getAttributes().get("login"), receiveMessage.getMessage(), "PRIVATE");
                if(isActiveUser(receiveMessage.getReceiver())){
                    sendPrivateMessage(receiveMessage.getReceiver(), sendMessage);
                }else{
                    webSocketService.savePrivateMessage(receiveMessage.getReceiver(), sendMessage);
                }
                break;
            }
            case "LOGOUT":{
                SendMessage sendMessage = createSendMessage((String)session.getAttributes().get("login"), "",  "LOGOUT");
                sendPrivateMessage((String)session.getAttributes().get("login"), sendMessage);
                removeFromActiveUsers((String)session.getAttributes().get("login"));
                sendAllChangeActiveList();
                break;
            }

        }
    }

    private String getErrorMessage(Set<ConstraintViolation<ReceiveMessage>> violations){
        return violations.iterator().next().getMessage();
    }

    private SendMessage createSendMessage(String from, String message, String type){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setType(type);
        sendMessage.setMessage(message);
        sendMessage.setSender(from);
        sendMessage.setUsersLogin(null);
        return sendMessage;
    }

    private void sendPrivateMessage(String to, SendMessage sendMessage){
        try {
           activeUsers.get(to).sendMessage(new TextMessage(mapper.writeValueAsString(sendMessage)));
        } catch (IOException e) {
           throw new RuntimeException(e);
        }
    }

    private void sendAllChangeActiveList() {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setType("LIST");
        sendMessage.setUsersLogin(new ArrayList<>(activeUsers.keySet()));
        sendAll(sendMessage);


    }

    private void sendAll(SendMessage sendMessage) {
        try {
            for (Map.Entry<String, WebSocketSession> item : activeUsers.entrySet()) {
                TextMessage textMessage = new TextMessage(mapper.writeValueAsString(sendMessage));
                item.getValue().sendMessage(textMessage);
            }
        } catch (IOException e) {
            new RuntimeException(e);

        }
    }

    private boolean isActiveUser(String user){
        return activeUsers.keySet().stream().filter(u -> u.equals(user)).findFirst().isPresent();
    }

    private void removeFromActiveUsers(String user){
        activeUsers.remove(user);
    }
}
