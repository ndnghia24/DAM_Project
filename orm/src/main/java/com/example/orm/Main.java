package com.example.orm;

import com.example.orm.entities.User;
import com.example.orm.repositories.EntityRepository;

public class Main {
    public static void main(String[] args) {
        try {
            EntityRepository<User, Integer> userRepository = 
                new EntityRepository<>(User.class);

            User user = new User();
            user.setId(1);
            user.setUsername("JohnDoe");
            user.setPassword("password123");
            userRepository.save(user);

            userRepository.findAll().forEach(u -> 
                System.out.println("User ID: " + u.getId() + ", Name: " + u.getUsername()));

            userRepository.findById(1).ifPresent(u -> 
                System.out.println("Found user: " + u.getUsername()));

            // cannot delete because of primary key constraint
            // userRepository.delete(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}