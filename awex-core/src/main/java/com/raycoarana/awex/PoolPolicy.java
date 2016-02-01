package com.raycoarana.awex;

import com.raycoarana.awex.policy.LinearWithRealTimePriority;
import com.raycoarana.awex.state.PoolState;

public abstract class PoolPolicy {

    public static final PoolPolicy DEFAULT = new LinearWithRealTimePriority();

    private PoolManager mPoolManager;

    public void initialize(PoolManager poolManager) {
        mPoolManager = poolManager;
        onStartUp();
    }

    public void createQueue(int queueId) {
        mPoolManager.createQueue(queueId);
    }

    public void queueTask(int queueId, Task task) {
        mPoolManager.queueTask(queueId, task);
    }

    public void executeImmediately(Task task) {
        mPoolManager.executeImmediately(task);
    }

    public void createWorker(int queueId) {
        mPoolManager.createWorker(queueId);
    }

    /**
     * The pool is starting-up, its time to create the basic work queues and workers
     */
    public abstract void onStartUp();

    /**
     * Cada vez que un trabajo se añade, se saca un snapshot del estado de Awex (estado de hilos,
     * tasks en ejecución y estado de colas. La política debe decidir cola destino y prioridad
     * dentro de la cola. La política puede crear hilos y colas. A los hilos se les puede asignar
     * una afinidad. Los trabajos se pueden redireccionar, es decir, si ya existe en ejecución
     * o en un queue un trabajo con los mismos parámetros que además es determinista (mismos
     * parámetros dan lugar a mismo resultado), se puede en vez de encolar el trabajo, crear
     * un Pipe, el cual redireccionará el resultado de la tarea en ejecución a la tarea añadida,
     * sin consumir un hilo de ejecución.
     *
     * @param poolState
     * @param task
     */
    public abstract void onTaskAdded(PoolState poolState, Task task);

    /**
     * Cuando un task acaba, la política recibe otro evento con otra foto (task acabada no
     * incluida en la foto, pasada como parámetro).
     *
     * @param poolState
     * @param task
     */
    public abstract void onTaskFinished(PoolState poolState, Task task);

    /**
     * Las tareas pueden tener un tiempo maximo de espera en cola. Si este tiempo se cumple,
     * salta este evento acompañado de otra foto del pool y la tarea en cuestión, que se saca de
     * la cola previamente. Es responsabilidad de la política del pool volver a añadir a otro pool
     * este trabajo si sigue teniendo sentido ejecutarlo. También puede abortarlo.
     *
     * @param poolState
     * @param task
     */
    public abstract void onTaskQueueTimeout(PoolState poolState, Task task);

    /**
     * Las tareas tienen un tiempo máximo de ejecución. Si este tiempo se cumple antes de acabar,
     * salta este evento acompañado de la tarea y foto del pool.
     *
     * @param poolState
     * @param task
     */
    public abstract void onTaskExecutionTimeout(PoolState poolState, Task task);

}