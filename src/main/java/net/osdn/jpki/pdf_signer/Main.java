package net.osdn.jpki.pdf_signer;

import javafx.application.Platform;
import net.osdn.util.javafx.application.SingletonApplication;

public class Main {

	public static void main(String[] args) {
		// 画面の一部が再描画されずに白くなってしまうバグを回避するために、prism.dirtyopts=false を指定しています。
		System.setProperty("prism.dirtyopts", "false");

		Platform.setImplicitExit(false);
		SingletonApplication.launch(MainApp.class, args);

		// プロセス終了
		// この時点で main (id=1) と DestroyJavaVM の他に AWT-Shutdown などの非デーモンスレッドが残っていることがあります。
		// 非デーモンスレッドによってプロセスの終了が遅くなることがあります。
		// macOS では特に顕著で main メソッドの復帰からプロセス終了まで 5秒程度かかることもあります。
		// この影響で App Store の審査で「メインウィンドウを閉じてもプロセスが終了していない」というリジェクトを受けました。
		// 対処として、非デーモンスレッドが残っていても すばやくプロセスを終了できるように System.exit(0) を呼ぶようにしました。
		//
		// exewrap で二重起動を防止している場合は最初の launch が復帰して isStopped == true になったスレッドで System.exit(0) を呼びます。
		// isStopped == false の場合は、二重起動時のウィンドウを最前面にするだけの main メソッド呼び出しであり、System.exit(0) を呼び出してはいけません。
		if(SingletonApplication.isStopped()) {
			System.exit(0);
		}
	}
}
