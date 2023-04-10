package com.caovy2001.chatbot.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("user")
@org.springframework.data.elasticsearch.annotations.Document(indexName = "user")
public class UserEntity extends BaseEntity{
    @Id
    private String id;

    @Field("username")
    @org.springframework.data.elasticsearch.annotations.Field(type = FieldType.Text, name = "username")
    @Indexed
    private String username;

    @Field("password")
    @org.springframework.data.elasticsearch.annotations.Field(type = FieldType.Text, name = "password")
    private String password;

    @Field("fullname")
    @org.springframework.data.elasticsearch.annotations.Field(type = FieldType.Text, name = "fullname")
    private String fullname;

    @Field("secret_key")
    @org.springframework.data.elasticsearch.annotations.Field(type = FieldType.Text, name = "secret_key")
    @Indexed
    private String secretKey;

    @Field("token")
    @org.springframework.data.elasticsearch.annotations.Field(type = FieldType.Text, name = "token")
    @Indexed
    private String token;
}
