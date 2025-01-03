package com.example.orm;

import com.example.orm.gateways.UserTableGateway;
import com.example.orm.gateways.PostTableGateway;
import com.example.orm.gateways.CommentTableGateway;

public class CustomORM {
    private static CustomORM instance;
    private UserTableGateway userGateway;
    private PostTableGateway postGateway;
    private CommentTableGateway commentGateway;

    private CustomORM() {
        // Private constructor to prevent direct instantiation
    }

    public static CustomORM getInstance() {
        if (instance == null) {
            instance = new CustomORM();
        }
        return instance;
    }

    public static UserTableGateway User() {
        CustomORM orm = getInstance();
        if (orm.userGateway == null) {
            orm.userGateway = new UserTableGateway();
        }
        return orm.userGateway;
    }

    public static PostTableGateway Post() {
        CustomORM orm = getInstance();
        if (orm.postGateway == null) {
            orm.postGateway = new PostTableGateway();
        }
        return orm.postGateway;
    }

    public static CommentTableGateway Comment() {
        CustomORM orm = getInstance();
        if (orm.commentGateway == null) {
            orm.commentGateway = new CommentTableGateway();
        }
        return orm.commentGateway;
    }
} 