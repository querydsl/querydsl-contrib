package com.querydsl.dynamodb.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMarshaller;

public class GenderConverter implements DynamoDBMarshaller<User.Gender> {

    @Override
    public String marshall(User.Gender gender) {
        if (gender == null) {
            return null;
        }

        return gender.name();
    }

    @Override
    public User.Gender unmarshall(Class<User.Gender> clazz, String gender) {
        if (gender == null) {
            return null;
        }

        return Enum.valueOf(clazz, gender);
    }
}