# awex

[![Build Status](https://travis-ci.org/raycoarana/awex.svg?branch=master)](https://travis-ci.org/raycoarana/awex)
[![Coverage Status](https://coveralls.io/repos/raycoarana/awex/badge.svg?branch=master&service=github)](https://coveralls.io/github/raycoarana/awex?branch=master)

AWEX (Android Work EXecutor) is a thread pool to execute tasks that uses Promises to deliver results. Promises that can be cancelled, can be combined or even can process collections in parallel automatically.

How to use it?
--------------
First thing you need to do is setup an Awex object, that will be your thread pool. You could create some provider that will keep an instance to the Awex object implementing a Singleton pattern or you could implement it with your DI framework.

```java
public class AwexProvider {

    private static final int MIN_THREADS = 2;
    private static final int MAX_THREADS = 4;

    private static Awex sInstance = null;

    public static synchronized Awex get() {
        if (sInstance == null) {
            sInstance = new Awex(new AndroidUIThread(), new AndroidLogger(), MIN_THREADS, MAX_THREADS);
        }
        return sInstance;
    }

}
```

Once you have an Awex object, all you need to do to start is submit tasks. A _Task_ is an object like the classic Runnable but with some extras. To start with a simple example, let's create a task, submit to the thread pool and do something when it finishes.

```java
Awex awex = AwexProvider.get();
awex.submit(new Task<Integer>() {
    @Override
    protected Integer run() throws InterruptedException {
        //Do some heavy task here

        return 42; //Return some result
    }
}).done(new DoneCallback<Integer>() {
    @Override
    public void onDone(Integer result) {
        Log.i("Awex", "Result to the task execution is: " + result);
    }
});
```

If your task doesn't return anything, you could use _VoidTask_ instead and have a cleaner code than having to use _Task<Void>_.

```java
awex.submit(new VoidTask() {
    @Override
    protected void runWithoutResult() throws InterruptedException {
        //Do some heavy task here
    }
}).done(new DoneCallback<Void>() {
    @Override
    public void onDone(Void result) {
        Log.i("Awex", "Task finished successfully!");
    }
});
```

Promises
--------
Inspired by [JDeferred library](https://github.com/jdeferred/jdeferred), Awex use Promises to let you add subtasks to your task on all possible scenarios, when it finishes correctly, when it fails or in any case.

```java
awex.submit(someTask)
    .done(new DoneCallback<Integer>() {
        @Override
        public void onDone(Integer result) {
            //Task finishes correctly
        }
    })
    .fail(new FailCallback() {
        @Override
        public void onFail(Exception exception) {
            //Task fails
        }
    })
    .progress(new ProgressCallback() {
        @Override
        public void onProgress(float progress) {
            //Task done some progress
        }
    })
    .always(new AlwaysCallback() {
        @Override
        public void onAlways() {
            //Task either finishes correctly or fail
        }
    });
```

But if you are in Android, many times you want that some of that callbacks gets executed in the main thread to update the UI. For that purpose, Awex provide the same interfaces prefixed by _UI_. Everytime an _UI_ callback is added, its code is executed in the main thread.

```java
awex.submit(someTask)
    .done(new UIDoneCallback<Integer>() {
        @Override
        public void onDone(Integer result) {
            view.setValue(result); //All it's ok, show the result to the user
        }
    })
    .fail(new UIFailCallback() {
        @Override
        public void onFail(Exception exception) {
            view.showError(); //Can't get the value, show the error message
        }
    })
    .progress(new UIProgressCallback() {
        @Override
        public void onProgress(float progress) {
            view.updateProgress(progress);
        }
    })
    .always(new UIAlwaysCallback() {
        @Override
        public void onAlways() {
            view.hideLoading(); //In any case, hide the loading animation
        }
    });
```

Waiting for results
-------------------
You could get the result of the promise at any time, blocking your current thread until it's finished.

```java
Promise<Integer> promise = awex.submit(...);
try {
    Integer result = promise.getResult();
} catch (Exception ex) {
    //Task fails with exception ex
}
```

But you can get a default value in case of error

```java
Promise<Integer> promise = awex.submit(...);
Integer result = promise.getResultOrDefault(42); //will return 42 when the task fails
```

Cancelling tasks through promises
---------------------------------
A problem you could face when using promises on Android is that sometimes, when the user leaves some Activity or Fragment, any task that is running in the background to load information don't need to be completed and can be cancelled.
Even more, you must cancel it and remove any reference to the UI, so you don't leak the Activity or Fragment. For that and many other cases, Awex lets you cancel tasks using the associated promise.
Tasks can be interrupted or not when gets cancelled, tasks can query if they are already cancelled or not, so they can stop doing things once cancelled in a gracefully way.
In any case, after a promise/task is cancelled, no done/fail/always callback will be executed.

```java
public void onCreate() {
    ...

    //Load data to be shown
    mPromise = awex.submit(new Task<Integer>() {
        @Override
        protected Integer run() throws InterruptedException {
            return 42;
        }
    }).done(new DoneCallback<Integer>() {
        @Override
        public void onDone(Integer result) {
            //Task finishes correctly
        }
    });
}

public void onDestroy() {
    //On destroy, ensure any pending task is cancelled
    mPromise.cancelTask();
}
```

Tasks and promises could also be cancelled interrupting the worker thread. In that case, the thread will be removed from the thread pool and interrupted. A new thread will be created in the thread pool as soon as a new task is submitted.

```java
public void onCancelSave() {
    mPromise.cancelTask(/* mayInterrupt */true); //Tries to abort the worker thread
}
```

You could even know when some task in cancelled and add a callback for that case. That will be the only callback that will be executed when a promise/task is cancelled. Remember to not reference any Activity/Fragment from a cancelled callback or it will be leaked!

```java
mPromise = AwexProvider.get().submit(new Task<Integer>() {
    @Override
    protected Integer run() throws InterruptedException {
        //Do some work...

        if (isCancelled()) return -1; //This result won't be processed never

        //Do more work...

        return 42;
    }
}).done(new DoneCallback<Integer>() {
    @Override
    public void onDone(Integer result) {
        //Task finishes correctly
    }
}).cancel(new CancelCallback() {
    @Override
    public void onCancel() {
        //Task cancelled...
    }
});
```

Not just promises
-----------------
There are many more things you can do with Awex and its promises.

###OR operator

```java
awex.submit(new Task<Integer>() {
    @Override
    protected Integer run() throws InterruptedException {
        throw new RuntimeException("Some error");
    }
})
.or(awex.of(42))
.done(new DoneCallback<Integer>() {
    @Override
    public void onDone(Integer result) {
        //This will be executed always with 42
    }
});
```
Awex supports doing an OR of many promises using the _anyOf()_ method in the Awex object.

###AND operator

```java
awex.submit(new Task<Integer>() {
    @Override
    protected Integer run() throws InterruptedException {
        return 42;
    }
})
.and(awex.of(43))
.done(new DoneCallback<Collection<Integer>>() {
    @Override
    public void onDone(Collection<Integer> result) {
        //result will contain 42 and 43
    }
});
```
Awex supports doing an AND of many promises using the _allOf()_ method in the Awex object.

###AfterAll operator

```java
awex.afterAll(awex.of(41), awex.of(42), awex.of(43))
    .done(new DoneCallback<MultipleResult<Integer>>() {
        @Override
        public void onDone(MultipleResult<Integer> result) {
            //MultipleResult contains all values or error of all promises
        }
    });
```

###Filter operator

###Map operator

Download
--------

Download via Maven:
```xml
<dependency>
  <groupId>com.raycoarana.awex</groupId>
  <artifactId>awex-android</artifactId>
  <version>X.X.X</version>
</dependency>
```
or Gradle:
```groovy
compile 'com.raycoarana.awex:awex-android:X.X.X'
```

License
-------

    Copyright 2015 Rayco Araña

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.