package com.example.orm.gateways;

import java.util.List;
import java.util.Optional;

import com.example.orm.entities.User;
import com.example.orm.repositories.EntityRepository;

public class UserTableGateway implements ITableGateway<User> {
    private final EntityRepository<User, Integer> repository;
    
    public UserTableGateway() {
        this.repository = new EntityRepository<>(User.class);
    }

    @Override
    public void insert(User user) throws Exception {
        repository.save(user);
    }

    @Override
    public void update(User user) throws Exception {
        repository.save(user);
    }

    @Override
    public void delete(int id) throws Exception {
        findById(id).ifPresent(user -> {
            try {
                repository.delete(user);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Optional<User> findById(int id) throws Exception {
        return repository.findById(id);
    }

    @Override
    public List<User> findAll() throws Exception {
        return repository.findAll();
    }

    @Override
    public EntityRepository<User, Integer>.ConditionBuilder<User> findWithConditions() {
        return repository.findWithConditions();
    }
    
    // Others methods
    public List<User> findByUsername(String username) throws Exception {
        return repository.findWithConditions()
            .where(User.Attributes.USERNAME, username)
            .execute();
    }
    
    public boolean exists(String username) throws Exception {
        List<User> users = findByUsername(username);
        return !users.isEmpty();
    }
}

// public interface UserTableGateway extends ITableGateway<User> {
//     List<User> findByUsername(String username) throws Exception;
//     boolean exists(String username) throws Exception;
// }