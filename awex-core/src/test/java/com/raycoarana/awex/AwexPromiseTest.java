package com.raycoarana.awex;

import com.raycoarana.awex.callbacks.AlwaysCallback;
import com.raycoarana.awex.callbacks.CancelCallback;
import com.raycoarana.awex.callbacks.DoneCallback;
import com.raycoarana.awex.callbacks.FailCallback;
import com.raycoarana.awex.callbacks.UIAlwaysCallback;
import com.raycoarana.awex.callbacks.UICancelCallback;
import com.raycoarana.awex.callbacks.UIDoneCallback;
import com.raycoarana.awex.callbacks.UIFailCallback;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AwexPromiseTest {

	private static final long SOME_WORK_ID = 42;
	private static final Integer SOME_RESULT = 666;
	private static final Integer SOME_DEFAULT_RESULT = 999;

	@SuppressWarnings("ThrowableInstanceNeverThrown")
	private static final Exception REJECT_EXCEPTION = new Exception();

	@Mock
	private Awex mAwex;

	@Mock
	private Work mWork;

	@Mock
	private Logger mLogger;

	@Mock
	private UIThread mUIThread;

	@Mock
	private DoneCallback<Integer> mDoneCallback;

	@Mock
	private UIDoneCallback<Integer> mUIDoneCallback;

	@Mock
	private FailCallback mFailCallback;

    @Mock
    private CancelCallback mCancelCallback;

	@Mock
	private UIFailCallback mUIFailCallback;

	@Mock
	private AlwaysCallback mAlwaysCallback;

	@Mock
	private UIAlwaysCallback mUIAlwaysCallback;

    @Mock
    private UICancelCallback mUICancelCallback;

	private AwexPromise<Integer> mPromise;

    @Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void shouldCreateAValidPromise() {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);

		assertEquals(Promise.STATE_PENDING, mPromise.getState());
	}

    @Test(expected = IllegalStateException.class)
    public void shouldFailToGetResultBeforeResolved() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.getResult();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailToGetResultOrDefaultBeforeResolved() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.getResultOrDefault(SOME_DEFAULT_RESULT);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailToGetResultOfCancelledPromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.cancel();
        mPromise.getResult();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailToGetResultOrDefaultOfCancelledPromise() throws Exception {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.cancel();
        mPromise.getResultOrDefault(SOME_DEFAULT_RESULT);
    }

    @Test
	public void shouldResolveThePromise() throws Exception {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.resolve(SOME_RESULT);

		assertEquals(Promise.STATE_RESOLVED, mPromise.getState());
		assertEquals(SOME_RESULT, mPromise.getResult());
		assertEquals(SOME_RESULT, mPromise.getResultOrDefault(SOME_DEFAULT_RESULT));
	}

	@Test(expected = IllegalStateException.class)
	public void shouldFailToResolveTwiceThePromise() {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.resolve(SOME_RESULT);
		mPromise.resolve(SOME_RESULT);
	}

	@Test
	public void shouldExecuteDoneCallbackAddedBeforeResolveThePromise() throws Exception {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.done(mDoneCallback)
				.always(mAlwaysCallback);
		mPromise.resolve(SOME_RESULT);

		verify(mDoneCallback).onDone(SOME_RESULT);
		verify(mAlwaysCallback).onAlways();
	}

	@Test
	public void shouldExecuteDoneCallbackAddedAfterResolveThePromise() throws Exception {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.resolve(SOME_RESULT);
		mPromise.done(mDoneCallback)
				.always(mAlwaysCallback);

		verify(mDoneCallback).onDone(SOME_RESULT);
		verify(mAlwaysCallback).onAlways();
	}

	@Test
	public void shouldExecuteUIDoneCallbackAddedBeforeResolveThePromise() throws Exception {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.done(mUIDoneCallback)
				.always(mUIAlwaysCallback);
		mPromise.resolve(SOME_RESULT);

		verify(mUIThread, times(2)).post(any(Runnable.class));
		verify(mUIDoneCallback).onDone(SOME_RESULT);
		verify(mUIAlwaysCallback).onAlways();
	}

	@Test
	public void shouldExecuteUIDoneCallbackInCurrentThreadWhenResolvingFromUIThread() throws Exception {
        setUpAwex();
        givenThatUIThreadIsCurrentThread();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.resolve(SOME_RESULT);
		mPromise.done(mUIDoneCallback)
				.always(mUIAlwaysCallback);

		verify(mUIThread, never()).post(any(Runnable.class));
        verify(mAwex, never()).submit(any(Runnable.class));
		verify(mUIDoneCallback).onDone(SOME_RESULT);
		verify(mUIAlwaysCallback).onAlways();
	}

    @Test
    public void shouldExecuteDoneCallbackInBackgroundThreadWhenResolvingFromUIThread() throws Exception {
        setUpAwex();
        givenThatUIThreadIsCurrentThread();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.resolve(SOME_RESULT);
        mPromise.done(mDoneCallback)
                .always(mAlwaysCallback);

        verify(mUIThread, never()).post(any(Runnable.class));
        verify(mAwex, times(2)).submit(any(Runnable.class));
        verify(mDoneCallback).onDone(SOME_RESULT);
        verify(mAlwaysCallback).onAlways();
    }

	@Test
	public void shouldExecuteUIDoneCallbackAddedAfterResolveThePromise() throws Exception {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.resolve(SOME_RESULT);
		mPromise.done(mUIDoneCallback)
				.always(mUIAlwaysCallback);

		verify(mUIThread, times(2)).post(any(Runnable.class));
		verify(mUIDoneCallback).onDone(SOME_RESULT);
		verify(mUIAlwaysCallback).onAlways();
	}

    @Test
    public void shouldContinueExecutingOtherDoneCallbacksWhenACallbackFails() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.done(buildFailingDoneCallback());
        mPromise.done(mDoneCallback);
        mPromise.resolve(SOME_RESULT);

        verify(mDoneCallback).onDone(SOME_RESULT);
        verify(mLogger).e(anyString(), any(Exception.class));
    }

    @SuppressWarnings("unchecked")
    private DoneCallback<Integer> buildFailingDoneCallback() {
        DoneCallback<Integer> callback = mock(DoneCallback.class);
        doThrow(Exception.class).when(callback).onDone(anyInt());
        return callback;
    }

    @Test
    public void shouldContinueExecutingOtherAlwaysCallbacksWhenACallbackFails() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.always(buildFailingAlwaysCallback());
        mPromise.always(mAlwaysCallback);
        mPromise.resolve(SOME_RESULT);

        verify(mAlwaysCallback).onAlways();
        verify(mLogger).e(anyString(), any(Exception.class));
    }

    @SuppressWarnings("unchecked")
    private AlwaysCallback buildFailingAlwaysCallback() {
        AlwaysCallback callback = mock(AlwaysCallback.class);
        doThrow(Exception.class).when(callback).onAlways();
        return callback;
    }

	@Test
	public void shouldExecuteFailCallbackAddedBeforeRejectThePromise() throws Exception {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.fail(mFailCallback)
				.always(mAlwaysCallback);
		mPromise.reject(REJECT_EXCEPTION);

		verify(mFailCallback).onFail(REJECT_EXCEPTION);
		verify(mAlwaysCallback).onAlways();
	}

	@Test
	public void shouldExecuteFailCallbackAddedAfterRejectThePromise() throws Exception {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.reject(REJECT_EXCEPTION);
		mPromise.fail(mFailCallback)
				.always(mAlwaysCallback);

		verify(mFailCallback).onFail(REJECT_EXCEPTION);
		verify(mAlwaysCallback).onAlways();
	}

	@Test
	public void shouldExecuteUIFailCallbackAddedBeforeRejectThePromise() throws Exception {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.fail(mUIFailCallback)
				.always(mUIAlwaysCallback);
		mPromise.reject(REJECT_EXCEPTION);

		verify(mUIThread, times(2)).post(any(Runnable.class));
		verify(mUIFailCallback).onFail(REJECT_EXCEPTION);
		verify(mUIAlwaysCallback).onAlways();
	}

	@Test
	public void shouldExecuteUIFailCallbackAddedAfterRejectThePromise() throws Exception {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.reject(REJECT_EXCEPTION);
		mPromise.fail(mUIFailCallback)
				.always(mUIAlwaysCallback);

		verify(mUIThread, times(2)).post(any(Runnable.class));
		verify(mUIFailCallback).onFail(REJECT_EXCEPTION);
		verify(mUIAlwaysCallback).onAlways();
	}

    @Test
    public void shouldContinueExecutingOtherFailCallbacksWhenACallbackFails() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.fail(buildFailingFailCallback());
        mPromise.fail(mFailCallback);
        mPromise.reject(new RuntimeException());

        verify(mFailCallback).onFail(any(Exception.class));
        verify(mLogger).e(anyString(), any(Exception.class));
    }

    private FailCallback buildFailingFailCallback() {
        FailCallback callback = mock(FailCallback.class);
        doThrow(Exception.class).when(callback).onFail(any(Exception.class));
        return callback;
    }

    @Test
    public void shouldExecuteFailCallbackInBackgroundThreadWhenResolvingFromUIThread() throws Exception {
        setUpAwex();
        givenThatUIThreadIsCurrentThread();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.reject(new RuntimeException());
        mPromise.fail(mFailCallback)
                .always(mAlwaysCallback);

        verify(mUIThread, never()).post(any(Runnable.class));
        verify(mAwex, times(2)).submit(any(Runnable.class));
        verify(mFailCallback).onFail(any(RuntimeException.class));
        verify(mAlwaysCallback).onAlways();
    }

	@Test(expected = IndexOutOfBoundsException.class)
	public void shouldRejectThePromise() throws Exception {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.reject(new IndexOutOfBoundsException());

		assertEquals(Promise.STATE_REJECTED, mPromise.getState());
		mPromise.getResult();
	}

	@Test(expected = IllegalStateException.class)
	public void shouldFailToRejectTwiceThePromise() throws Exception {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.reject(new IndexOutOfBoundsException());
		mPromise.reject(new IndexOutOfBoundsException());
	}

	@Test
	public void shouldGetDefaultValueWhenPromiseIsRejected() {
        setUpAwex();

		mPromise = new AwexPromise<>(mAwex, mWork);
		mPromise.reject(new IndexOutOfBoundsException());

		assertEquals(SOME_DEFAULT_RESULT, mPromise.getResultOrDefault(SOME_DEFAULT_RESULT));
	}

    @Test
    public void shouldExecuteCancelCallbackWhenCancelPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.cancel(mCancelCallback);
        mPromise.cancel();

        verify(mCancelCallback).onCancel();
    }

    @Test
    public void shouldExecuteCancelCallbackAfterCancelPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.cancel();
        mPromise.cancel(mCancelCallback);

        verify(mCancelCallback).onCancel();
    }

    @Test
    public void shouldContinueExecutingOtherCancelCallbacksWhenACallbackFails() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.cancel(buildFailingCancelCallback());
        mPromise.cancel(mCancelCallback);
        mPromise.cancel();

        verify(mCancelCallback).onCancel();
        verify(mLogger).e(anyString(), any(Exception.class));
    }

    private CancelCallback buildFailingCancelCallback() {
        CancelCallback callback = mock(CancelCallback.class);
        doThrow(Exception.class).when(callback).onCancel();
        return callback;
    }

    @Test
    public void shouldExecuteUICancelCallbackWhenCancelPromise() {
        setUpAwex();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.cancel(mUICancelCallback);
        mPromise.cancel();

        verify(mUIThread).post(any(Runnable.class));
        verify(mUICancelCallback).onCancel();
    }

    @Test
    public void shouldExecuteCancelCallbackInBackgroundThreadWhenCancellingFromUIThread() throws Exception {
        setUpAwex();
        givenThatUIThreadIsCurrentThread();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.cancel(mCancelCallback);
        mPromise.cancel();

        verify(mUIThread, never()).post(any(Runnable.class));
        verify(mAwex).submit(any(Runnable.class));
        verify(mCancelCallback).onCancel();
    }

    @Test
    public void shouldExecuteCancelCallbackInBackgroundThreadAfterCancellingFromUIThread() throws Exception {
        setUpAwex();
        givenThatUIThreadIsCurrentThread();

        mPromise = new AwexPromise<>(mAwex, mWork);
        mPromise.cancel();
        mPromise.cancel(mCancelCallback);

        verify(mUIThread, never()).post(any(Runnable.class));
        verify(mAwex).submit(any(Runnable.class));
        verify(mCancelCallback).onCancel();
    }

    private void setUpAwex() {
        givenAnAwex();
        givenAWork();
        givenAUIThread();
    }

	private void givenAnAwex() {
		when(mAwex.provideLogger()).thenReturn(mLogger);
		when(mAwex.provideUIThread()).thenReturn(mUIThread);

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				((Work)invocation.getArguments()[0]).execute();
				return null;
			}
		}).when(mAwex).submit(isA(Work.class));

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				((Runnable)invocation.getArguments()[0]).run();
				return null;
			}
		}).when(mAwex).submit(isA(Runnable.class));
	}

	private void givenAWork() {
		when(mWork.getId()).thenReturn(SOME_WORK_ID);
	}

	private void givenThatUIThreadIsCurrentThread() {
		when(mUIThread.isCurrentThread()).thenReturn(true);
	}

	private void givenAUIThread() {
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				((Runnable)invocation.getArguments()[0]).run();
				return null;
			}
		}).when(mUIThread).post(isA(Runnable.class));
	}

}