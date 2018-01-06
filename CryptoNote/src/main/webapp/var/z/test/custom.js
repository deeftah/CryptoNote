// Appel en fin de <head>
class Custom {
	static ready() { // appel quand AppHomes est ready()
		console.log(App.Util.log(App.lib("start_mode" + App.mode), 3000));
	}
}
App.Custom = Custom;
