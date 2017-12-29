// Appel en fin de <head>
class Custom {
	static ready() { // appel quand AppHomes est ready()
		App.messageBox.show("Started !!!", 5000);
	}
}
App.Custom = Custom;
