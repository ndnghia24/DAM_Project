package com.example.orm.gateways;

import java.util.List;
import java.util.Optional;

import com.example.orm.repositories.EntityRepository;

public interface ITableGateway<T> {
    void insert(T entity) throws Exception;
    void update(T entity) throws Exception; 
    void delete(int id) throws Exception;
    Optional<T> findById(int id) throws Exception;
    List<T> findAll() throws Exception;
    EntityRepository<T, Integer>.ConditionBuilder<T> findWithConditions();
}

//fasdfasdfasdfsa//