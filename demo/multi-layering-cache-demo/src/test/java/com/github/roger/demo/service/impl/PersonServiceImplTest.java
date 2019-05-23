package com.github.roger.demo.service.impl;

import com.github.roger.demo.MultiLayeringCacheDemoStarterTest;
import com.github.roger.demo.entity.Person;
import com.github.roger.demo.service.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

@Slf4j
public class PersonServiceImplTest extends MultiLayeringCacheDemoStarterTest {

    @Autowired
    private PersonService personService;

    @Test
    public void testSave() {
        Person p = new Person(1, "name1", 12, "address1");
        //保存之后，写入缓存
        personService.save(p);
        //从缓存中抓取数据，一次虽然真正的方法返回结果的id为2 ，实际得到的结果
        //id为1
        Person person = personService.findOne(p);
        Assert.assertEquals(person.getId(), 1);
    }

    @Test
    public void testRemove() {
        Person p = new Person(5, "name1", 12, "address1");
        personService.save(p);
        log.info("缓存中一个缓存 id = 5 的 Person 对象");

        personService.remove(5L);
        log.info("删除一个缓存 id = 5 的 Person 对象");

        Person person = personService.findOne(p);
        log.info("从数据库中抓取一个 id = 2 的对象，并存入缓存");
        Assert.assertEquals(person.getId(), 2);
    }
}