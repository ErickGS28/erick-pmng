package com.example.peopleagain.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeopleRepository extends JpaRepository<People, Long> {
    // NO escribes nada aquí. Ya tienes gratis:
    // findAll(), findById(id), save(obj), deleteById(id), etc.
}
