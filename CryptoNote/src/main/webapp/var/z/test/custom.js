// Appel en fin de <head>
class Custom {
	
	// appel quand AppHomes est ready()
	static async ready() { 
		console.log(App.Util.log(App.lib("start_mode" + App.mode), 3000));
		Custom.declareNom("Daniel Sportès");
	}
	
	// appel par l'appui du bouton refresh
	static syncRequested() {
		// todo
		App.appHomes.resetSync();
	}
	
	// retourne le credential courant
	static credential() {
		const c = {account:this.account, key:this.key}
		if (this.sudo) c.sudo = this.sudo;
		return c;
	}
	
	static declarePhoto(photo) {
		this.userPhoto = photo;
		App.appHomes.setUser(this.userName, this.userPhoto);
	}

	static declareNom(nom) {
		this.userName = nom;
		App.appHomes.setUser(this.userName, this.userPhoto);
	}

	static declareSudo(sudo) {
		this.sudo = sudo; //App.sudo
		App.appHomes.setSudo(this.sudo);
	}

}
App.Custom = Custom;
// App.superman = "data:image/png;base64,iVBO ...";