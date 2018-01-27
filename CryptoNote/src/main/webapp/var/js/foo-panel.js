class FooPanel extends Polymer.Element {
	static get is() { return "foo-panel"; }
  
	static get properties() { return {
    	  lang:{type:String, value:App.lang},
      };
	}
	
	constructor() {
		super();
		App.setMsg("fr", "foo_truc", "Machin");		
	}
	
	ready() {
		super.ready();
	}

	lib(code, lang) { return App.lib(code); }
		
	show() {
		this.$.panel.open();
	}
	
	close() { 
		this.$.panel.close(); 
	}
		
}
customElements.define(FooPanel.is, FooPanel);
