package com.kevo.photoGalleryV2.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
@RequestMapping("/stress-test")
public class StressTestController {

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    private final int instanceId = (new Random()).nextInt(100) + 1;
    private final AtomicBoolean isStressTestRunning = new AtomicBoolean(false);
    private final AtomicInteger targetCpuUsage = new AtomicInteger(50);
    private volatile ScheduledExecutorService stressTestExecutor;

    @GetMapping
    public String showStressTestPage(Model model) {
        populateModelWithSystemInfo(model);
        return "stress-test";
    }

    @PostMapping("/start")
    public String startStressTest(@RequestParam int cpuPercentage,
                                  @RequestParam int duration,
                                  @RequestParam String testType,
                                  RedirectAttributes redirectAttributes) {

        if (isStressTestRunning.get()) {
            redirectAttributes.addFlashAttribute("error", "Stress test is already running!");
            return "redirect:/stress-test";
        }

        try {
            targetCpuUsage.set(cpuPercentage);
            isStressTestRunning.set(true);

            startCpuStressTest(cpuPercentage, duration, testType);

            redirectAttributes.addFlashAttribute("message",
                    String.format("Stress test started: %d%% CPU usage for %d minutes (%s)",
                            cpuPercentage, duration, testType));

        } catch (Exception e) {
            isStressTestRunning.set(false);
            redirectAttributes.addFlashAttribute("error", "Failed to start stress test: " + e.getMessage());
        }

        return "redirect:/stress-test";
    }

    @PostMapping("/stop")
    @ResponseBody
    public Map<String, Object> stopStressTest() {
        Map<String, Object> response = new HashMap<>();

        try {
            if (stressTestExecutor != null) {
                stressTestExecutor.shutdownNow();
                stressTestExecutor = null;
            }
            isStressTestRunning.set(false);

            response.put("success", true);
            response.put("message", "Stress test stopped successfully");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error stopping stress test: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/status")
    @ResponseBody
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isRunning", isStressTestRunning.get());
        status.put("targetCpuUsage", targetCpuUsage.get());
        status.put("currentCpuUsage", getCurrentCpuUsage());
        status.put("needsRefresh", false); // For auto-refresh functionality
        return status;
    }

    private void startCpuStressTest(int cpuPercentage, int durationMinutes, String testType) {
        int cores = Runtime.getRuntime().availableProcessors();
        stressTestExecutor = Executors.newScheduledThreadPool(cores);

        // Calculate work and sleep times based on desired CPU percentage
        long workTime = cpuPercentage; // milliseconds of work
        long sleepTime = 100 - cpuPercentage; // milliseconds of sleep

        // Start stress test threads for each core
        for (int i = 0; i < cores; i++) {
            stressTestExecutor.scheduleWithFixedDelay(() -> {
                if (isStressTestRunning.get()) {
                    performCpuStress(workTime, sleepTime, testType);
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        }

        // Schedule automatic stop after duration
        executorService.schedule(() -> {
            if (stressTestExecutor != null) {
                stressTestExecutor.shutdownNow();
                stressTestExecutor = null;
            }
            isStressTestRunning.set(false);
        }, durationMinutes, TimeUnit.MINUTES);
    }

    private void performCpuStress(long workTime, long sleepTime, String testType) {
        long startTime = System.currentTimeMillis();

        switch (testType) {
            case "MIXED_LOAD":
                performMixedLoadWork(workTime);
                break;
            case "BURST_PATTERN":
                performBurstPatternWork(workTime);
                break;
            default:
                performCpuIntensiveWork(workTime);
        }

        // Sleep for the remaining time
        long elapsedTime = System.currentTimeMillis() - startTime;
        long remainingSleepTime = sleepTime - elapsedTime;

        if (remainingSleepTime > 0) {
            try {
                Thread.sleep(remainingSleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void performCpuIntensiveWork(long workTime) {
        long endTime = System.currentTimeMillis() + workTime;
        double result = 0;

        while (System.currentTimeMillis() < endTime && isStressTestRunning.get()) {
            // Perform mathematical operations to consume CPU
            result += Math.sqrt(Math.random() * 1000);
            result += Math.sin(Math.random() * Math.PI);
            result += Math.cos(Math.random() * Math.PI);
            result = result % 1000; // Prevent overflow
        }
    }

    private void performMixedLoadWork(long workTime) {
        long endTime = System.currentTimeMillis() + workTime;

        while (System.currentTimeMillis() < endTime && isStressTestRunning.get()) {
            // Mix of CPU and memory operations
            double[] array = new double[1000];
            for (int i = 0; i < array.length; i++) {
                array[i] = Math.random() * i;
            }

            // Sort the array (CPU + memory intensive)
            java.util.Arrays.sort(array);
        }
    }

    private void performBurstPatternWork(long workTime) {
        long endTime = System.currentTimeMillis() + workTime;

        while (System.currentTimeMillis() < endTime && isStressTestRunning.get()) {
            // Intense burst for short periods
            for (int i = 0; i < 10000 && isStressTestRunning.get(); i++) {
                Math.pow(Math.random(), Math.random());
            }

            // Brief pause
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void populateModelWithSystemInfo(Model model) {
        model.addAttribute("systemStatus", "ACTIVE");
        model.addAttribute("instanceId", instanceId);
        model.addAttribute("availableCores", Runtime.getRuntime().availableProcessors());
        model.addAttribute("currentCpuUsage", getCurrentCpuUsage() + "%");
        model.addAttribute("stressTestStatus", isStressTestRunning.get() ? "RUNNING" : "IDLE");
    }

    private int getCurrentCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean =
                        (com.sun.management.OperatingSystemMXBean) osBean;
                double cpuUsage = sunOsBean.getProcessCpuLoad() * 100;
                return cpuUsage > 0 ? (int) cpuUsage : 0;
            }
        } catch (Exception e) {
            // Fallback if unable to get CPU usage
        }
        return 0;
    }
}