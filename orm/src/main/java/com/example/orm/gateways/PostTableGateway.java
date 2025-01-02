package com.example.orm.gateways;

import java.util.List;
import java.util.Optional;
import com.example.orm.entities.Post;
import com.example.orm.repositories.EntityRepository;
import com.example.orm.repositories.EntityRepository.ConditionBuilder;

public class PostTableGateway implements ITableGateway<Post> {
    private final EntityRepository<Post, Integer> repository;
    
    public PostTableGateway() {
        this.repository = new EntityRepository<>(Post.class);
    }
    
    @Override
    public void insert(Post post) throws Exception {
        repository.save(post);
    }
    
    @Override
    public void update(Post post) throws Exception {
        repository.save(post);
    }
    
    @Override
    public void delete(int id) throws Exception {
        findById(id).ifPresent(post -> {
            try {
                repository.delete(post);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public Optional<Post> findById(int id) throws Exception {
        return repository.findById(id);
    }
    
    @Override
    public List<Post> findAll() throws Exception {
        return repository.findAll();
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ConditionBuilder findWithConditions() {
        return repository.findWithConditions();
    }
    
    // Other methods
    public List<Post> findByUserId(String userId) throws Exception {
        return repository.findWithConditions()
            .where(Post.Attributes.USER_ID, userId)
            .execute();
    }
    
    public List<Post> findByContent(String content) throws Exception {
        return repository.findWithConditions()
            .where(Post.Attributes.CONTENT, content)
            .execute();
    }
}