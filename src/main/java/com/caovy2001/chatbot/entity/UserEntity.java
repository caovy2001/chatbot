package com.caovy2001.chatbot.entity;

import com.caovy2001.chatbot.service.user.enumeration.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("user")
public class UserEntity extends BaseEntity{
    @Id
    private String id;

    @Field("username")
    @Indexed
    private String username;

    @Field("password")
    private String password;

    @Field("fullname")
    private String fullname;

    @Field("phone")
    private String phone;

    @Field("zalo_group_link")
    private String zaloGroupLink;

    @Field("google_meet_link")
    private String googleMeetLink;

    @Field("secret_key")
    @Indexed
    private String secretKey;

    @Field("token")
    @Indexed
    private String token;

    @Field("role")
    @Builder.Default()
    private UserRole role = UserRole.USER;

    @Field("created_date")
    private Long createdDate;
}
