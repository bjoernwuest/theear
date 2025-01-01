package software.theear;

public enum SystemExitReasons {
	NoDatabaseConnectivity(-1, "Could not connect to data base. Application requires a data base connection."),
	CannotPrepareDatabase(-2, "Data base fitness check failed and cannot automatically fix. See application logs for details."),
	FailedToPersistPermission(-3, "Failed to persist detected and used permission in data base.");

  public final int ExitCode;
	public final String Message;

	private SystemExitReasons(int Code, String Msg) {
	  int c = 0 == Code ? -1 : Code;
	  this.ExitCode = (c == Math.abs(c)) ? -c : c;
	  this.Message = Msg;
	}
}
