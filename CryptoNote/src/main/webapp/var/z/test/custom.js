// Appel en fin de <head>
class Custom {
	static ready() { // appel quand AppHomes est ready()
		console.log("Custom.ready() de test")
	}
}
App.Custom = Custom;
