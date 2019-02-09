package com.infopulse.service.dataservice;

import com.infopulse.entity.Ban;
import com.infopulse.entity.ChatUser;
import com.infopulse.exception.UserAlreadyBanedException;
import com.infopulse.exception.UserCanNotBeUnBanedException;
import com.infopulse.exception.UserNotFoundException;
import com.infopulse.repository.BanRepository;
import com.infopulse.repository.ChatUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BanDataService {

    @Autowired
    private BanRepository banRepository;

    @Autowired
    private ChatUserRepository chatUserRepository;


    @Transactional
    public void addUserToBan(String login){
        ChatUser chatUser = chatUserRepository.findByLogin(login);
        if(chatUser ==null){
            throw new UserNotFoundException(String.format("User with login %s does not exist", login));

        }

        if(chatUser.getBan()!=null){
            throw new UserAlreadyBanedException("User already was banned");
        }

        Ban ban =new Ban();
        ban.setChatuser(chatUser);
        banRepository.save(ban);

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying
    public void removeFromBan(String login){
        ChatUser chatUser = chatUserRepository.findByLogin(login);
        if(chatUser == null){
            throw new UserNotFoundException(String.format("User with login %s does not exist", login));

        }

        if(chatUser.getBan()== null){
            throw new UserCanNotBeUnBanedException("User can not be deleted from ban");
        }
        Ban ban = chatUser.getBan();
        chatUser.setBan(null);
        banRepository.deleteById(ban.getId());
    }

    public boolean verify(String login){
        ChatUser chatUser = chatUserRepository.findByLogin(login);
        if(chatUser == null){
            throw new UserNotFoundException(String.format("User with login %s does not exist", login));

        }
        Ban ban = chatUser.getBan();
        return ban !=null;
    }
}
