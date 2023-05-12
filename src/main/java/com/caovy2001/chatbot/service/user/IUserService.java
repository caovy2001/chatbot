package com.caovy2001.chatbot.service.user;

import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.service.IBaseService;
import com.caovy2001.chatbot.service.ResponseBase;
import com.caovy2001.chatbot.service.user.command.CommandGetListUser;
import com.caovy2001.chatbot.service.user.command.CommandUserLogin;
import com.caovy2001.chatbot.service.user.command.CommandUserSignUp;
import com.caovy2001.chatbot.service.user.response.ResponseUserLogin;
import com.caovy2001.chatbot.service.user.response.ResponseUserSignUp;

import java.util.List;

public interface IUserService extends IBaseService {
    ResponseUserLogin login(CommandUserLogin command);

    UserEntity getById(String id);

    ResponseUserSignUp signUp(CommandUserSignUp commandUserSignUp) throws Exception;

    UserEntity getBySecretKey(String secretKey);

    ResponseBase loginFromDataEverywhere(CommandUserLogin command);

    List<UserEntity> getList(CommandGetListUser command);

    List<UserEntity> getAll_vul();
}
