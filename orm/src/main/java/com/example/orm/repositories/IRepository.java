package com.example.orm.repositories;

import java.util.List;
import java.util.Optional;

public interface IRepository<T, ID> {
    void save(T entity) throws Exception;
    void delete(T entity) throws Exception;
    Optional<T> findById(ID id) throws Exception;
    List<T> findAll() throws Exception;
}