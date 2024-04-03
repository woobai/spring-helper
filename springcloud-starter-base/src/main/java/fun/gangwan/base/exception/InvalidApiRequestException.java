package fun.gangwan.base.exception;

import java.io.Serializable;

/**
 *
 * 代表非法请求异常<br>
 *
 * <p>在请求标头不正确、或者无凭据时抛出此异常</p>
 *
 * <p>向客户端返回http code为401</p>
 *
 * @author ZhouYi
 * @since 2021/08/09
 * @version 1.0.0
 */
public class InvalidApiRequestException extends RuntimeException implements Serializable {

	private static final long serialVersionUID = -5334302778718454357L;

	public InvalidApiRequestException() {
		super();
	}

	public InvalidApiRequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidApiRequestException(String message) {
		super(message);
	}

	public InvalidApiRequestException(Throwable cause) {
		super(cause);
	}
}
