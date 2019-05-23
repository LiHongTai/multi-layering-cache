package com.github.roger.demo.controller;

import com.github.roger.demo.entity.Person;
import com.github.roger.demo.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PersonController {
    @Autowired
    PersonService personService;

    @RequestMapping("/put")
    public long put(@RequestBody Person person) {
        Person p = personService.save(person);
        return p.getId();
    }

    @RequestMapping("/able")
    public Person cacheable(@RequestBody Person person) {

        return personService.findOne(person);
    }

    @RequestMapping("/evit")
    public String evit(@RequestBody Person person) {

        personService.remove(person.getId());
        return "ok";
    }

}
