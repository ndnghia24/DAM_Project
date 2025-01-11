package com.example.orm;

import java.util.List;
import java.util.Map;

import com.example.orm.entities.Post;
import com.example.orm.entities.User;
import com.example.orm.querybuilder.SQLCondition;
import com.example.orm.repositories.EntityRepository;

public class Main {
    public static void main(String[] args) {
        try {
            User user = new User();
            user.setId(1);
            user.setUsername("JohnDoe");
            user.setPassword("password123");
            CustomORM.User().insert(user);
            System.out.println("\nInserted User: " + user.getUsername());

            User user2 = new User();
            user2.setId(2);
            user2.setUsername("JaneDoe");
            user2.setPassword("password456");
            CustomORM.User().insert(user2);
            System.out.println("Inserted User: " + user2.getUsername());

            List<User> users = CustomORM.User().findAll();
            System.out.println("\nAll Users:");
            users.forEach(u -> 
                System.out.println("User ID: " + u.getId() + ", Name: " + u.getUsername())
            );

            CustomORM.User().findById(1).ifPresent(u -> 
                System.out.println("[+] Found user by Id: " + u.getUsername())
            );

            List<User> filteredUsers = CustomORM.User().findWithConditions()
                .where(User.Attributes.USERNAME, "JohnDoe")
                .where(User.Attributes.ID, "1")
                .groupBy(User.Attributes.USERNAME, User.Attributes.ID)
                .having(new SQLCondition(SQLCondition.AggregateFunction.SUM, User.Attributes.ID)
                    .withOperator(">", "0"))
                .execute();

            System.out.println("\n[+] Found users by conditions: ");
            if (filteredUsers.isEmpty()) {
                System.out.println("No users found");
            } else {
                filteredUsers.forEach(u -> 
                    System.out.println("User ID: " + u.getId() + ", Name: " + u.getUsername())
                );
            }

            List<Map<String, Object>> userResults = EntityRepository.executeRawQuery(
                "SELECT * FROM Users WHERE username = 'JohnDoe' AND id = '1' " +
                "GROUP BY username, id HAVING SUM(id) > 0"
            );

            System.out.println("\n[+] Raw query results for Users: ");
            userResults.forEach(row -> {
                StringBuilder sb = new StringBuilder();
                row.forEach((column, value) -> 
                    sb.append(column).append(": ").append(value).append(" | ")
                );
                System.out.println(sb.toString());
            });

            user.setPassword("newPassword123");
            CustomORM.User().update(user);
            System.out.println("\nUpdated User ID 1's password.");

            CustomORM.User().findById(1).ifPresent(u -> 
                System.out.println("User ID: " + u.getId() + ", New Password: " + u.getPassword())
            );

            CustomORM.User().delete(2);
            System.out.println("\nDeleted User with ID 2.");

            List<User> usersAfterDelete = CustomORM.User().findAll();
            System.out.println("All Users After Deletion:");
            usersAfterDelete.forEach(u -> 
                System.out.println("User ID: " + u.getId() + ", Name: " + u.getUsername())
            );

            Post post1 = new Post();
            post1.setId(1);
            post1.setUserId("1");
            post1.setContent("Hello World! This is my first post.");
            CustomORM.Post().insert(post1);
            System.out.println("\nInserted Post ID: " + post1.getId() + ", Content: " + post1.getContent());

            Post post2 = new Post();
            post2.setId(2);
            post2.setUserId("1");
            post2.setContent("Another post by JohnDoe.");
            CustomORM.Post().insert(post2);
            System.out.println("Inserted Post ID: " + post2.getId() + ", Content: " + post2.getContent());

            Post post3 = new Post();
            post3.setId(3);
            post3.setUserId("3");
            post3.setContent("This post is linked to a non-existing user.");
            try {
                CustomORM.Post().insert(post3);
                System.out.println("Inserted Post ID: " + post3.getId() + ", Content: " + post3.getContent());
            } catch (Exception e) {
                System.err.println("Failed to insert Post ID: " + post3.getId() + ". Reason: " + e.getMessage());
            }

            List<Post> posts = CustomORM.Post().findAll();
            System.out.println("\nAll Posts:");
            posts.forEach(p -> 
                System.out.println("Post ID: " + p.getId() + ", User ID: " + p.getUserId() + ", Content: " + p.getContent())
            );

            CustomORM.Post().findById(1).ifPresent(p -> 
                System.out.println("\nFound Post by ID 1: " + p.getContent())
            );

            post1.setContent("Updated content for my first post.");
            CustomORM.Post().update(post1);
            System.out.println("\nUpdated Post ID 1's content.");

            CustomORM.Post().findById(1).ifPresent(p -> 
                System.out.println("Post ID: " + p.getId() + ", New Content: " + p.getContent())
            );

            CustomORM.Post().delete(2);
            System.out.println("\nDeleted Post with ID 2.");

            List<Post> postsAfterDelete = CustomORM.Post().findAll();
            System.out.println("All Posts After Deletion:");
            postsAfterDelete.forEach(p -> 
                System.out.println("Post ID: " + p.getId() + ", User ID: " + p.getUserId() + ", Content: " + p.getContent())
            );

            System.out.println("\nPost-User Relationships:");
            CustomORM.Post().findAll().forEach(p -> {
                try {
                    User associatedUser = CustomORM.User().findById(Integer.parseInt(p.getUserId())).orElse(null);
                    if (associatedUser != null) {
                        System.out.println("Post ID: " + p.getId() + " is authored by User: " + associatedUser.getUsername());
                    } else {
                        System.out.println("Post ID: " + p.getId() + " has no associated User.");
                    }
                } catch (NumberFormatException nfe) {
                    System.err.println("Invalid User ID format for Post ID: " + p.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            CustomORM.User().delete(1);
            System.out.println("\nDeleted User with ID 1.");

            List<Post> postsAfterUserDelete = CustomORM.Post().findAll();
            System.out.println("All Posts After Deleting User ID 1:");
            if (postsAfterUserDelete.isEmpty()) {
                System.out.println("No posts found.");
            } else {
                postsAfterUserDelete.forEach(p -> 
                    System.out.println("Post ID: " + p.getId() + ", User ID: " + p.getUserId() + ", Content: " + p.getContent())
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}