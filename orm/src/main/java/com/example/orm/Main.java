package com.example.orm;

import java.util.List;
import java.util.Map;

import com.example.orm.entities.User;
import com.example.orm.gateways.UserTableGateway;
import com.example.orm.querybuilder.SQLCondition;
import com.example.orm.repositories.EntityRepository;

public class Main {
    public static void main(String[] args) {
        try {
            UserTableGateway userGateway = new UserTableGateway();

            User user = new User();
            user.setId(1);
            user.setUsername("JohnDoe");
            user.setPassword("password123");
            userGateway.insert(user);

            userGateway.findAll().forEach(u -> 
                System.out.println("User ID: " + u.getId() + ", Name: " + u.getUsername()));

            userGateway.findById(1).ifPresent(u -> 
                System.out.println("[+] Found user by Id: " + u.getUsername()));

            // cannot delete because of primary key constraint
            // userGateway.delete(1);

            // call findWithConditions
            List<User> users = userGateway.findWithConditions()
                .where(User.Attributes.USERNAME, "JohnDoe")
                .where(User.Attributes.ID, "1")
                .groupBy(User.Attributes.USERNAME, User.Attributes.ID)
                .having(new SQLCondition(SQLCondition.AggregateFunction.SUM, User.Attributes.ID)
                    .withOperator(">", "0"))
                .execute();

                
            // print result
            System.out.println("[+] Found users by conditions: ");
            if (users.isEmpty()) {
                System.out.println("No users found");
            } else {
                users.forEach(u -> 
                    System.out.println("User ID: " + u.getId() + ", Name: " + u.getUsername()));
            }


            // Raw query like call findWithConditions
            List<Map<String, Object>> results = EntityRepository.executeRawQuery(
                "SELECT * FROM Users WHERE username = 'JohnDoe' AND id = '1' " +
                "GROUP BY username, id HAVING SUM(id) > 0");

            // Print results
            System.out.println("[+] Raw query results: ");
            results.forEach(row -> {
                StringBuilder sb = new StringBuilder();
                row.forEach((column, value) -> 
                    sb.append(column).append(": ").append(value).append(" | "));
                System.out.println(sb.toString());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}