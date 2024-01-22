package com.becoze.biback.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Queue test controller
 */
@RestController
@RequestMapping("/queue")
@Slf4j
@Profile({"dev", "local"})
public class QueueControllerTest {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;


    /**
     * submit a task to the thread pool for texting
     * @param task
     */
    @GetMapping("/add")
    public void add(String task){
        CompletableFuture.runAsync(() -> {
            System.out.println("Progressing task: " + task + " by " + Thread.currentThread().getName());

            try {
                // the task will remain 60 min (for test)
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, threadPoolExecutor);
    }

    @GetMapping("/get")
    public String get(){
        Map<String, Object> map = new HashMap<>();

        int size = threadPoolExecutor.getQueue().size();
        map.put("# of Tasks in the queue", size);

        long taskCount = threadPoolExecutor.getTaskCount();
        map.put("# of Tasks", taskCount);

        long completeTaskCount = threadPoolExecutor.getCompletedTaskCount();
        map.put("# of Completed tasks", completeTaskCount);

        int activeCount = threadPoolExecutor.getActiveCount();
        map.put("# of Active threads", activeCount);

        return JSONUtil.toJsonStr(map);
    }

}
