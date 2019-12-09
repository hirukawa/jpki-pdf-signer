デスクトップ・ブリッジを使ってJavaアプリをUWPアプリにしよう
https://blogs.osdn.jp/2018/11/28/java-uwp.html

(1) AppxConsole.bat を起動します。

(2) makepri createconfig /cf priconfig.xml /dq ja-JP

(3) makepri new /pr jpki-pdf-signer /cf priconfig.xml

(4) makeappx pack /d jpki-pdf-signer /p jpki-pdf-signer.appx /o /l /nv

- - - -
自己署名

(5) makecert -r -n "CN=MyName" -sv my-sample.pvk my-sample.cer

(6) pvk2pfx -pvk my-sample.pvk -spc my-sample.cer -pfx my-sample.pfx

(7) signtool sign /a /fd SHA256 /f my-sample.pfx jpki-pdf-signer.appx
