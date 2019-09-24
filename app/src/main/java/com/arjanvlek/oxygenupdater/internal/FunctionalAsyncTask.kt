package com.arjanvlek.oxygenupdater.internal

import android.os.AsyncTask

import java8.util.function.Consumer
import java8.util.function.Function

/**
 * Oxygen Updater - © 2017 Arjan Vlek
 */

class FunctionalAsyncTask<Params, Progress, Result> @JvmOverloads constructor(private val preExecuteFunction: Worker,
                                                                              private val backgroundFunction: Function<Array<Params>, Result>,
                                                                              private val postExecuteFunction: Consumer<Result>,
                                                                              private val progressUpdateFunction: Consumer<Array<Progress>>? = null) : AsyncTask<Params, Progress, Result>() {

    override fun doInBackground(params: Array<Params>): Result {
        return backgroundFunction.apply(params)
    }

    override fun onPreExecute() {
        preExecuteFunction.start()
    }

    override fun onPostExecute(result: Result) {
        postExecuteFunction.accept(result)
    }

    @SafeVarargs
    override fun onProgressUpdate(progress: Array<Progress>) {
        progressUpdateFunction?.accept(progress)
    }
}
