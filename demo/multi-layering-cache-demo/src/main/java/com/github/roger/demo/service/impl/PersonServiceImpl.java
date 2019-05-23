package com.github.roger.demo.service.impl;

import com.github.roger.annotation.CacheEvict;
import com.github.roger.annotation.Cacheable;
import com.github.roger.annotation.FirstCache;
import com.github.roger.annotation.SecondaryCache;
import com.github.roger.demo.entity.Person;
import com.github.roger.demo.service.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PersonServiceImpl implements PersonService {

    @Cacheable(value = "people" , key = "#person.id")
    @Override
    public Person save(Person person) {
        log.info("为id、key为:" + person.getId() + "数据做了缓存");
        return person;
    }

    @CacheEvict(value = "people", key = "#id")
    @Override
    public void remove(Long id) {
        log.info("删除了id、key为" + id + "的数据缓存");
    }

    @CacheEvict(value = "people", allEntries = true)
    @Override
    public void removeAll() {
        log.info("删除了所有缓存的数据缓存");
    }

    @Cacheable(value = "people", key = "#person.id",
            firstCache = @FirstCache(expireTime = 4),
            secondaryCache = @SecondaryCache(expireTime = 5, preloadTime = 1, forceRefresh = true))
    @Override
    public Person findOne(Person person) {
        Person p = new Person(2L, "name2", 12,"address2");
        log.info("为id、key为:" + p.getId() + "数据做了缓存");
        return p;
    }
}
