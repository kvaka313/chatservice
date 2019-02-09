package com.infopulse.service.controllerservices;

import com.infopulse.dto.ChatUserDto;
import com.infopulse.exception.IncorrectParameterException;
import com.infopulse.service.dataservice.BanDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BanControllerService {

    @Autowired
    private BanDataService banDataService;

    public void addUserToBan(ChatUserDto chatUserDto) {
        String login = chatUserDto.getLogin();
        if (login == null) {
            throw new IncorrectParameterException("Login is required");
        }

        banDataService.addUserToBan(login);
    }

    public void removeUserFromBan(String login){
        banDataService.removeFromBan(login);
    }

    public boolean verifyForBan(String login){
        return banDataService.verify(login);
    }



}
