package com.raycoarana.awex.callbacks;

import com.raycoarana.awex.Promise;

public interface ThenCallback<Arg, Result, Progress> {

    Promise<Result, Progress> then(Arg arg);

}
