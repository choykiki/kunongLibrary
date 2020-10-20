package kunong.android.library.concurrent;

public abstract class QThread<Params, Result> implements Runnable {

	private final Params[] params;
	private Result result;

	@SafeVarargs
	public QThread(Params... params) {
		this.params = params;
	}

	@Override
	public final void run() {
		onCompleted(result);
	}

	public final void execute() {
		result = run(params);
	}

	@SuppressWarnings("unchecked")
	public abstract Result run(Params... params);

	public abstract void onCompleted(Result result);

}
