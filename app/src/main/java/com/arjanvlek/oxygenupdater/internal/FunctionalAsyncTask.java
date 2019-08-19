package com.arjanvlek.oxygenupdater.internal;

import android.os.AsyncTask;

import java8.util.function.Consumer;
import java8.util.function.Function;

/**
 * Oxygen Updater - © 2017 Arjan Vlek
 */

@SuppressWarnings("WeakerAccess")
public class FunctionalAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

	private final Worker preExecuteFunction;
	private final Function<Params[], Result> backgroundFunction;
	private final Consumer<Progress[]> progressUpdateFunction;
	private final Consumer<Result> postExecuteFunction;

	public FunctionalAsyncTask(Worker preExecuteFunction, Function<Params[], Result> backgroundFunction, Consumer<Result> postExecuteFunction) {
		this(preExecuteFunction, backgroundFunction, postExecuteFunction, null);
	}

	public FunctionalAsyncTask(Worker preExecuteFunction, Function<Params[], Result> backgroundFunction, Consumer<Result> postExecuteFunction, Consumer<Progress[]> progressUpdateFunction) {
		this.postExecuteFunction = postExecuteFunction;
		this.preExecuteFunction = preExecuteFunction;
		this.backgroundFunction = backgroundFunction;
		this.progressUpdateFunction = progressUpdateFunction;
	}

	@Override
	protected Result doInBackground(Params[] params) {
		return backgroundFunction.apply(params);
	}

	@Override
	protected void onPreExecute() {
		preExecuteFunction.start();
	}

	@Override
	protected void onPostExecute(Result result) {
		postExecuteFunction.accept(result);
	}

	@SafeVarargs
	@Override
	protected final void onProgressUpdate(Progress... progress) {
		if (progressUpdateFunction != null) {
			progressUpdateFunction.accept(progress);
		}
	}
}
