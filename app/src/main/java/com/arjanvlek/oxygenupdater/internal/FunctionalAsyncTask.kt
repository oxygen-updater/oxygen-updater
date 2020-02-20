package com.arjanvlek.oxygenupdater.internal

import android.os.AsyncTask

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 * @author Arjan Vlek (github.com/arjanvlek)
 */
class FunctionalAsyncTask<Params, Progress, Result> @JvmOverloads constructor(
    private val preExecuteFunction: () -> Unit,
    private val backgroundFunction: KotlinFunction<Array<Params>, Result>,
    private val postExecuteFunction: KotlinCallback<Result>,
    private val progressUpdateFunction: KotlinCallback<Array<Progress>>? = null
) : AsyncTask<Params, Progress, Result>() {

    override fun doInBackground(params: Array<Params>) = backgroundFunction.invoke(params)

    override fun onPreExecute() = preExecuteFunction.invoke()

    override fun onPostExecute(result: Result) = postExecuteFunction.invoke(result)

    override fun onProgressUpdate(progress: Array<Progress>) {
        progressUpdateFunction?.invoke(progress)
    }

}
