package com.caovy2001.chatbot.api.admin;

import com.caovy2001.chatbot.entity.UserEntity;
import com.caovy2001.chatbot.model.Paginated;
import com.caovy2001.chatbot.service.intent.response.ResponseIntents;
import com.caovy2001.chatbot.service.user.IUserService;
import com.caovy2001.chatbot.service.user.command.CommandGetListUser;
import com.caovy2001.chatbot.service.user.command.CommandUserUpdate;
import com.caovy2001.chatbot.service.user.enumeration.UserRole;
import com.caovy2001.chatbot.service.user.response.ResponseUserAdminUpdate;
import org.elasticsearch.common.inject.internal.ErrorsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/user")
public class UserAdminAPI {
    @Autowired
    private IUserService userService;

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PostMapping("/get_pagination")
    public ResponseEntity<Paginated<UserEntity>> getByPagination(@RequestBody CommandGetListUser command) {
        try {
            command.setUserId("admin");
            command.setReturnFields(List.of("id", "username", "fullname", "phone", "zalo_group_link", "google_meet_link", "secret_key"));
            command.setRole(UserRole.USER);
            return ResponseEntity.ok(userService.getPaginatedList(command, UserEntity.class, CommandGetListUser.class));
        } catch (Exception e) {
            return ResponseEntity.ok(new Paginated<>(new ArrayList<>(), 1, 0, 0));
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PostMapping("/update-user-info")
    public ResponseEntity<ResponseUserAdminUpdate> updateUser(@RequestBody CommandUserUpdate command) {
        try {
            UserEntity user = userService.update(command);
            if (user != null) {
                return ResponseEntity.ok(ResponseUserAdminUpdate.builder()
                        .id(user.getId())
                        .fullname(user.getFullname())
                        .username(user.getUsername())
                        .googleMeetLink(user.getGoogleMeetLink())
                        .zaloGroupLink(user.getZaloGroupLink())
                        .build());
            }
            throw new Exception();
        } catch (Exception e) {
            return ResponseEntity.ok(userService.returnException(e.getMessage(), ResponseUserAdminUpdate.class));
        }
    }
}
