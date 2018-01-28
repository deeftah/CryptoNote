// Appel en fin de <head>
class Custom extends CustomTemplate{
	/* Surcharge des méthodes de js/custom.js spécifique à l'application et le cas échéant à l'organisation */
	// appel quand AppHomes est ready()
	static async ready() { 
		Custom.declareNom("Daniel Sportès"); // test
		console.log(App.Util.log(App.lib("start_mode" + App.mode), 3000));
	}

}
App.Custom = Custom;
// App.superman = "data:image/png;base64,iVBO ...";