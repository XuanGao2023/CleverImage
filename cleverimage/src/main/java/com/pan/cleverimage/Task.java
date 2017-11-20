package com.pan.cleverimage;

import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;


/**
 * Created by pan on 18/11/2017.
 */

public abstract class Task<TINPUT, TOUTPUT> implements Runnable {
    protected ExecutorService executorService;
    protected TaskLifeCycle<TINPUT, TOUTPUT> taskLifeCycle;
    protected Callback<TINPUT, TOUTPUT> callback;

    protected TINPUT input;
    protected TOUTPUT output;
    protected List<Task> nextTask = new ArrayList<>();
    public static final Handler handlerMainThread = new Handler();

    public Task() {
        this(null, null, null);
    }

    public Task(ExecutorService executorService) {
        this(executorService, null, null);

    }

    public Task(ExecutorService executorService, TINPUT input) {
        this(executorService, input, null);

    }

    public Task(ExecutorService executor, TINPUT input, TaskLifeCycle taskLifeCycle) {
        this.executorService = executor;
        this.input = input;
        this.taskLifeCycle = taskLifeCycle;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setInput(TINPUT input) {
        this.input = input;
    }

    public void setTaskLifeCycle(TaskLifeCycle<TINPUT, TOUTPUT> taskLifeCycle) {
        this.taskLifeCycle = taskLifeCycle;
    }

    public void setCallback(Callback<TINPUT, TOUTPUT> callback) {
        this.callback = callback;
    }

    public void setNextTasks(List<Task> list) {
        nextTask.addAll(list);
    }

    public void setNextTask(Task task) {
        nextTask.add(task);
    }

    public void start() {
        start(null);
    }

    public void start(TINPUT t) {
        if (executorService == null) {
            throw new IllegalArgumentException("executorService is null!");
        }
        if (t != null) {
            this.input = t;
        }
        //input can be null.
        executorService.submit(this);
    }

    abstract TOUTPUT OnProcessing(TINPUT input);

    @Override
    public void run() {
        try {
            if (taskLifeCycle != null) {
                taskLifeCycle.OnStart(input);
            }
            final TOUTPUT output = OnProcessing(input);
            if (taskLifeCycle != null) {
                taskLifeCycle.OnEnd(input, output);
            }
            if (callback != null) {
                handlerMainThread.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.OnFinish(input, output);
                    }
                });
            }
            for (Task task : nextTask) {
                task.start(output);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TINPUT getInput() {
        return input;
    }

    public TOUTPUT getOutput() {
        return output;
    }

    /**
     * The member function is called on sub thread.
     *
     * @param <CINPUT>
     * @param <COUTPUT>
     */
    public interface TaskLifeCycle<CINPUT, COUTPUT> {
        void OnStart(CINPUT input);

        void OnEnd(CINPUT input, COUTPUT output);
    }

    /**
     * The member function is called on main thread.
     *
     * @param <CINPUT>
     * @param <COUTPUT>
     */
    public interface Callback<CINPUT, COUTPUT> {
        void OnFinish(CINPUT input, COUTPUT output);
    }
}
