package com.github.roger.demo.service;

import com.github.roger.demo.entity.Person;

public interface PersonService {

    Person save(Person person);

    void remove(Long id);

    void removeAll();

    Person findOne(Person person);
}
