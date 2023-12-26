package com.becoze.biback.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class yuAiManagerTest {
    @Resource
    private YuAiManager yuAiManager;

    @Test
    void doChat() {
        String res = yuAiManager.doChat(1709156902984093697L, "分析需求:分析网站用户的增长情况\n" +
                "原始数据:\n" +
                "日期,用户数\n" +
                "1号,10\n" +
                "2号,20\n" +
                "3号,30\n");
        System.out.println(res);
    }
}