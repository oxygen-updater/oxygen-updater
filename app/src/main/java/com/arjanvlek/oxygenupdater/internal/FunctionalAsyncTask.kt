package com.arjanvlek.oxygenupdater.internal

import android.os.AsyncTask
import java8.util.function.Function

/**
 * Oxygen Updater - © 2017 Arjan Vlek
 */
class FunctionalAsyncTask<Params, Progress, Result> @JvmOverloads constructor(
    private val preExecuteFunction: () -> Unit,
    private val backgroundFunction: Function<Array<Params>, Result>,
    private val postExecuteFunction: KotlinCallback<Result>,
    private val progressUpdateFunction: KotlinCallback<Array<Progress>>? = null
) : AsyncTask<Params, Progress, Result>() {

    override fun doInBackground(params: Array<Params>): Result {
        return backgroundFunction.apply(params)
    }

    override fun onPreExecute() {
        preExecuteFunction.invoke()
    }

    override fun onPostExecute(result: Result) {
        postExecuteFunction.invoke(result)
    }

    @SafeVarargs
    override fun onProgressUpdate(progress: Array<Progress>) {
        progressUpdateFunction?.invoke(progress)
    }

}
