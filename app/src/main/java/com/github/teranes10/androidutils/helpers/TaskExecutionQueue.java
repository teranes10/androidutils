package com.github.teranes10.androidutils.helpers;

import android.content.Context;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class TaskExecutionQueue {
    private static final String TAG = "TaskExecutionQueueService";
    private final Context _ctx;
    private final Supplier<Boolean> _canExecuteTask;
    private final Queue<Task> _tasks = new LinkedList<>();

    public TaskExecutionQueue(Context ctx) {
        this(ctx, null);
    }

    public TaskExecutionQueue(Context ctx, Supplier<Boolean> canExecuteTask) {
        this._ctx = ctx;
        this._canExecuteTask = canExecuteTask;
    }

    public void add(Task task) {
        CompletableFuture.runAsync(() -> {
            if (task == null) {
                return;
            }

            if (execute(task)) {
                return;
            }

            _tasks.add(task);
            Log.i(TAG, "addNewTask: added.");
        });
    }

    public void execute() {
        int size = _tasks.size();
        Log.i(TAG, "executeTasks: " + size);
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                Task task = _tasks.remove();
                execute(task);
            }
        }
    }

    private boolean execute(Task task) {
        boolean canExecute = _canExecuteTask == null || _canExecuteTask.get();
        if (canExecute) {
            try {
                task.execute(_ctx);
                Log.i(TAG, "addNewTask: executed.");
            } catch (Exception e) {
                Log.e(TAG, "addNewTask: ", e);
            }

            return true;
        }

        return false;
    }

    public interface Task {
        void execute(Context context);
    }
}
