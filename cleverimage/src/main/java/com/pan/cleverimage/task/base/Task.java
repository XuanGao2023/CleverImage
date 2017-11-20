package com.pan.cleverimage.task.base;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;


/**
 * Created by pan on 18/11/2017.
 */

public abstract class Task<TINPUT, TOUTPUT> implements Runnable {
    protected ExecutorService executorService;
    protected TaskLifeCycle<TINPUT, TOUTPUT> taskLifeCycle;
    protected Callback<TINPUT, TOUTPUT> taskCallback;
    protected Setting taskSetting = new Setting();
    protected TINPUT input;
    protected TOUTPUT output;
    protected List<Task> nextTask = new ArrayList<>();
    public static final Handler handlerMainThread = new Handler();

    public Task() {
        this(null, null, null, null);
    }

    public Task(ExecutorService executorService) {
        this(executorService, null, null, null);

    }

    public Task(ExecutorService executor, TINPUT input) {
        this(executor, input, null, null);
    }

    public Task(ExecutorService executor, TINPUT input, Setting setting, Callback callback) {
        this.executorService = executor;
        this.input = input;
        if (setting != null) {
            this.taskSetting.putAll(setting);
        }
        this.taskCallback = callback;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setInput(TINPUT input) {
        this.input = input;
    }

    public void setSetting(Setting setting) {
        this.taskSetting = setting;
    }

    public TINPUT getInput() {
        return input;
    }

    public TOUTPUT getOutput() {
        return output;
    }

    public Setting getTaskSetting() {
        return taskSetting;
    }

    public TaskLifeCycle setTaskLifeCycle(TaskLifeCycle<TINPUT, TOUTPUT> lifecycle) {
        if (taskLifeCycle != null) {
            TaskLifeCycle previous = taskLifeCycle;
            taskLifeCycle = lifecycle;
            return previous;
        }
        taskLifeCycle = lifecycle;
        return null;
    }

    public Callback setCallback(Callback<TINPUT, TOUTPUT> callback) {
        if (taskCallback != null) {
            Callback previous = taskCallback;
            taskCallback = callback;
            return previous;
        }
        taskCallback = callback;
        return null;
    }

    public void setNextTasks(List<Task> list) {
        nextTask.addAll(list);
    }

    public void setNextTask(Task task) {
        nextTask.add(task);
    }

    public void start() {
        start(null, null);
    }

    public void start(TINPUT t) {
        start(t, null);
    }

    public void start(Setting setting) {
        start(null, setting);
    }

    public void start(TINPUT t, Setting setting) {
        if (executorService == null) {
            throw new IllegalArgumentException("executorService is null!");
        }
        if (t != null) {
            this.input = t;
        }
        //will all all the settings to this.
        if (setting != null) {
            this.taskSetting.putAll(setting);
        }
        //input can be null.
        executorService.submit(this);
    }

    public abstract TOUTPUT OnProcessing(TINPUT input, Setting setting);

    @Override
    public void run() {
        try {
            if (taskLifeCycle != null) {
                taskLifeCycle.OnStart(input, taskSetting);
            }
            final TOUTPUT output = OnProcessing(input, taskSetting);
            if (taskLifeCycle != null) {
                taskLifeCycle.OnEnd(input, output, taskSetting);
            }
            if (taskCallback != null) {
                handlerMainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        taskCallback.OnFinish(input, output, taskSetting);
                    }
                });
            }
            //call with this one's setting.
            for (Task task : nextTask) {
                task.start(output, taskSetting);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The member function is called on sub thread.
     *
     * @param <CINPUT>
     * @param <COUTPUT>
     */
    public interface TaskLifeCycle<CINPUT, COUTPUT> {
        void OnStart(CINPUT input, Setting setting);

        void OnEnd(CINPUT input, COUTPUT output, Setting setting);
    }

    /**
     * The member function is called on main thread.
     *
     * @param <CINPUT>
     * @param <COUTPUT>
     */
    public interface Callback<CINPUT, COUTPUT> {
        void OnFinish(CINPUT input, COUTPUT output, Setting setting);
    }
}
