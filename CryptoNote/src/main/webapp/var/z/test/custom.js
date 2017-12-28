// Appel en fin de <head>
class Custom {
	static head() { // appel en fin de head
		console.log("Custom.head() de test")
	}
	
	static ready() { // appel quand AppHomes est ready()
		console.log("Custom.ready() de test")
	}
}
App.Main = Custom;
Custom.head();