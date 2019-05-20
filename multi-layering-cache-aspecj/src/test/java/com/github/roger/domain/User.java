package com.github.roger.domain;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.*;

@Data
@ToString
public class User {

    private long userId;

    private String name;

    private Address address;

    private String[] lastName;

    private List<String> lastNameList;

    private Set<String> lastNameSet;

    private int age;

    private double height;

    private BigDecimal income;

    private Date birthday;


    public User() {
        this.userId = 32L;
        this.name = "name";
        this.address = new Address();
        List<String> lastNameList = new ArrayList<String>();
        lastNameList.add("W");
        lastNameList.add("成都");
        this.lastNameList = lastNameList;
        this.lastNameSet = new HashSet<String>(lastNameList);
        this.lastName = new String[]{"w", "四川", "~！@#%……&*（）——+{}：“？》:''\">?《~!@#$%^&*()_+{}\\"};
        this.age = 122;
        this.height = 18.2;
        this.income = new BigDecimal(22.22);
        this.birthday = new Date();
    }
}
