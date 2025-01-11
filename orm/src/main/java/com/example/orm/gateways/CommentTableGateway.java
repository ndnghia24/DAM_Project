package com.example.orm.gateways;

import java.util.List;
import java.util.Optional;

import com.example.orm.entities.Comment;
import com.example.orm.repositories.EntityRepository;
import com.example.orm.repositories.EntityRepository.ConditionBuilder;

public class CommentTableGateway implements ITableGateway<Comment> {
    private final EntityRepository<Comment, Integer> repository;
    
    public CommentTableGateway() {
        this.repository = new EntityRepository<>(Comment.class);
    }
    
    @Override
    public void insert(Comment comment) throws Exception {
        repository.save(comment);
    }
    
    @Override
    public void update(Comment comment) throws Exception {
        repository.save(comment);
    }
    
    @Override
    public void delete(int id) throws Exception {
        findById(id).ifPresent(comment -> {
            try {
                repository.delete(comment);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Override
    public Optional<Comment> findById(int id) throws Exception {
        return repository.findById(id);
    }
    
    @Override
    public List<Comment> findAll() throws Exception {
        return repository.findAll();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ConditionBuilder findWithConditions() {
        return repository.findWithConditions();
    }
    
    // Other methods
    public List<Comment> findByPostId(int postId) throws Exception {
        return repository.findWithConditions()
            .where(Comment.Attributes.POST_ID, String.valueOf(postId))
            .execute();
    }
    
    public List<Comment> findByUserId(String userId) throws Exception {
        return repository.findWithConditions()
            .where(Comment.Attributes.USER_ID, userId)
            .execute();
    }
    
    public List<Comment> findByContent(String content) throws Exception {
        return repository.findWithConditions()
            .where(Comment.Attributes.CONTENT, content)
            .execute();
    }
}

// public interface CommentTableGateway extends ITableGateway<Comment> {
//     List<Comment> findByPostId(int postId) throws Exception;
//     List<Comment> findByUserId(String userId) throws Exception;
//     List<Comment> findByContent(String content) throws Exception;
// }