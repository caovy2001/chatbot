package com.caovy2001.chatbot.entity.es;

import com.caovy2001.chatbot.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(indexName = "user")
public class UserEntityES extends BaseEntity {
    @Id
    private String id;

    @Field(type = FieldType.Text, name = "username")
    private String username;

    @Field(type = FieldType.Text, name = "password")
    private String password;

    @Field(type = FieldType.Text, name = "fullname")
    private String fullname;

    @Field(type = FieldType.Text, name = "secret_key")
    private String secretKey;

    @Field(type = FieldType.Text, name = "token")
    private String token;
}

