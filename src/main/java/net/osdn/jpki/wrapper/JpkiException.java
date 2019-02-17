package net.osdn.jpki.wrapper;

import java.io.IOException;

public class JpkiException extends IOException {
	private static final long serialVersionUID = 1L;

	public static final int NTE_PROVIDER_DLL_FAIL     = 0x8009001D;
	public static final int NTE_KEYSET_NOT_DEF        = 0x80090019;
	public static final int SCARD_E_NOT_READY         = 0x80100010;
	public static final int SCARD_W_CANCELLED_BY_USER = 0x8010006E;
	
	private int errorCode;
	private int winErrorCode;
	private String message;
	private String localizedMessage;
	
	public JpkiException(int errorCode, int winErrorCode, Throwable cause) {
		super(cause);
		this.errorCode = errorCode;
		this.winErrorCode = winErrorCode;
		String[] messages = getMessages(errorCode, winErrorCode);
		if(messages != null) {
			this.message = messages[0];
			this.localizedMessage = messages[1];
		} else {
			String errorInfo = String.format(" (ErrorCode=%d, WinErrorCode=%d)", errorCode, winErrorCode);
			this.message = cause.getMessage() + errorInfo;
			this.localizedMessage = cause.getLocalizedMessage() + errorInfo;
		}
	}
	
	public JpkiException(String message, String localizedMessage, Throwable cause) {
		super(cause);
		this.message = message;
		if(localizedMessage != null) {
			this.localizedMessage = localizedMessage;
		} else {
			this.localizedMessage = message;
		}
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
	public int getWinErrorCode() {
		return winErrorCode;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String getLocalizedMessage() {
		return localizedMessage;
	}
	
	public static String[] getMessages(int errorCode, int winErrorCode) {
		switch(winErrorCode) {
		case NTE_PROVIDER_DLL_FAIL: return new String[] {
			"The provider DLL file could not be loaded or failed to initialize.",
			"プロバイダー DLL を初期化できませんでした。" };
		case NTE_KEYSET_NOT_DEF: return new String[] {
			"The requested provider does not exist.",
			"暗号サービス プロバイダ (CSP) が正しく設定されていません。" };
		case SCARD_E_NOT_READY: return new String[] {
			"The reader or smart card is not ready to accept commands.",
			"読み取り装置またはスマート カードは、コマンドを受け取る準備ができていません。" };
		case SCARD_W_CANCELLED_BY_USER: return new String[] {
			"The action was cancelled by the user.",
			"ユーザーによって操作は取り消されました。" };
		}
		return null;
	}
	
	
/*
SCARD_W_CANCELLED_BY_USER
パスワード入力が利用者によってキャンセルされた。

SCARD_E_NOT_READY
R/W 接 不備、IC カード未挿入等のため ICカードに接 できない。

SCARD_E_UNKNOWN_CARD
IC カード種別が相違している
。※IC カード種別相違と判定されるケースは以下の通り。
(1)CSP 名 称 が ”JPKI Crypto Service Provider”かつ住基カード以外の IC カードが 入されている場合
(2)CSP 名 称 が ”JPKI Crypto Service Provider for Sign”かつ個人番号カード以外の IC カードが 入されている場合
(3)CSP 名 称 が ”JPKI Crypto Service Provider for Auth”かつ個人番号カード以外の IC カードが 入されている場合

SCARD_W_CHV_BLOCKED 利用者パスワードがロックしている。
*/
}
