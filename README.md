# Quick application updater

Very small utiltiy to help testers quickly download and install updates of testing applications on their devices.

To make this app work, you need to have the HTTP server that holds prebuilt APK files and compse the repo.json file (or make a dynamical generator of its content) that will tell this application which applications should be in the list, and which URLs can be used to obtain each of them.

On Android 7+ the HTTPS is used to download files. On Android 6 and older, the plain HTTP is used as it's tricky to use modern HTTPS here.
