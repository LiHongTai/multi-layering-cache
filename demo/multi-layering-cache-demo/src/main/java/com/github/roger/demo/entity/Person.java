package com.github.roger.demo.entity;

import lombok.*;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
public class Person {

    private long id;

    @NonNull
    private String name;
    @NonNull
    private Integer age;

    private String address;
}
