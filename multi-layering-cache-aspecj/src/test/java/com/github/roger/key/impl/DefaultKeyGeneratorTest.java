package com.github.roger.key.impl;


import org.junit.Test;

public class DefaultKeyGeneratorTest {


    @Test
    public void generate() {
        DefaultKeyGenerator defaultKeyGenerator = new DefaultKeyGenerator();

        System.out.println(defaultKeyGenerator.generate(null,null,null));
        System.out.println(defaultKeyGenerator.generate(null,null,"1"));
        System.out.println(defaultKeyGenerator.generate(null,null,"1","2"));
    }
}