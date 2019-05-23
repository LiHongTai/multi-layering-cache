package com.github.roger.demo.controller;


import com.alibaba.fastjson.JSON;
import com.github.roger.demo.MultiLayeringCacheDemoStarterTest;
import com.github.roger.demo.entity.Person;
import com.github.roger.demo.utils.OkHttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.junit.Test;

import java.io.IOException;

@Slf4j
public class PersonControllerTest extends MultiLayeringCacheDemoStarterTest {

    String host = "http://127.0.0.1:8081/";

    @Test
    public void testPut() throws IOException {
        Person person = new Person(1, "name1", 12, "address1");

        RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8"),
                JSON.toJSONString(person));

        String post = OkHttpClientUtil.post(host + "put",requestBody);

        log.info("返回结果 result = " + post);
    }
}