package com.raycoarana.awex;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class BasePromiseTest {

    private static final long SOME_WORK_ID = 42;

    @Mock
    protected Awex mAwex;
    @Mock
    protected Work mWork;
    @Mock
    protected Logger mLogger;
    @Mock
    protected UIThread mUIThread;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    protected void setUpAwex() {
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
                ((Work) invocation.getArguments()[0]).execute();
                return null;
            }
        }).when(mAwex).submit(isA(Work.class));

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(mAwex).submit(isA(Runnable.class));
    }

    private void givenAWork() {
        when(mWork.getId()).thenReturn(SOME_WORK_ID);
    }

    protected void givenThatUIThreadIsCurrentThread() {
        when(mUIThread.isCurrentThread()).thenReturn(true);
    }

    private void givenAUIThread() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(mUIThread).post(isA(Runnable.class));
    }
}
