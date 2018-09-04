package io.ibntab.geth;

/**
 * @author ke.meng created on 2018/8/22
 */
public enum JavaProfile {

	DEV, TEST, BETA, PROD;

	public Profile asScala() {
		if (this == DEV) {
			return io.ibntab.geth.Profile.Dev$.MODULE$;
		} else if (this == PROD) {
			return io.ibntab.geth.Profile.Prod$.MODULE$;
		} else if (this == BETA) {
			return io.ibntab.geth.Profile.Beta$.MODULE$;
		}
		return io.ibntab.geth.Profile.Test$.MODULE$;
	}
}
