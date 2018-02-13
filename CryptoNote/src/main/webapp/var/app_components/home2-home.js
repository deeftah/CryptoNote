class Home2Home extends Polymer.GestureEventListeners(Polymer.Element) {
	static get is() { return "home2-home"; }
      
	static get properties() {
		return {
			lang:{type:String, value:App.lang},
            owner: {type:String, value:'Daniel' },
        	build: {type:Number, value:App.build},
        	lkbower: {type:String, value:App.basevar + "bower.json"},
        	cols : {type:Array, value:[]},
        	dragSrcEl : {type:Object, value:null}
		};
	}
	
    constructor() {
        super();
        this.bonjour = App.bonjour ? App.bonjour : "Salut" ;
        this.errTest = new TypeError("(script) erreur de test");
	}
    
    ready() {
    	super.ready();
    }

	lib(code, lang) { return App.lib(code);}

    show(arg, previousPage) {
  	  this.arg = arg;
  	  this.previousPage = previousPage;
  	  this.$.photo.show(App.superman);
    }
    
    title() {
    	return "La belle page Home2, La belle page Home2, La belle page Home2";
    }
    
    async mayHide() {
  	  return true;
    }
    
    back() {
  	  App.appHomes.back();
    }
    
    home1() {
  	  App.appHomes.forward("z-home1", {texte:"Bonjour home1"});
    }

    home1b() {
    	App.appHomes.setPage("z-home1", {texte:"Bonjour home1"});
	}

	confirm2() {
		App.confirmBox.show("Ceci est un beau message.", "OK", "Bullshit")
		.then(() => {
			console.log("Confirm OK");
		}).catch(() => {
			console.log("Confirm KO");    		  
		})
	}
	
	async confirm1() {
		if (await App.confirmBox.show("Ceci est un TRES beau message.", App.lib("lu")))
			console.log("Confirm Lu");
		else
			console.log("Confirm KO2");
	}
	
	changeThemeA() { App.appHomes.setTheme("a"); }
    changeThemeB() { App.appHomes.setTheme("b"); }
	changeLangFR() { App.appHomes.setLang("fr"); }
    changeLangEN() { App.appHomes.setLang("en"); }

	onFileLoaded(e){
		console.log(e.detail.url);
		console.log(e.detail.url.length);
		if (e.detail.resized)
			console.log(e.detail.resized.length);
	}

    async bower() {
		const spin = this.$.spin;
		spin.start(this, "bower.json");
		setTimeout(() => {
			spin.progress("Traitement du serveur");
			setTimeout(() => {
				this.loadBower();
			}, 2000);    		  
		}, 2000);
	}
    
    async loadBower() {
		try {
			let r = await fetch(App.basevar + "bower.json");
			if (r.ok) {
				let t = await r.text();
				this.pingres = t.substring(0, 30);
			}
	    	this.$.spin.stop();
		} catch(e) {
	    	this.$.spin.stop();
	    	console.error(App.Util.error(e.message, 5000));
		}   	
    }
    
    kill() {
    	console.error(App.Util.error("Kill bower.json", 3000));
    }
    
	async ping(spin) {
		try {
			const r = await new Req().setOp("ping").setSpinner(spin).setTimeOut(6000).go();
			this.pingres = !r ? "json mal formé ?" : JSON.stringify(r);					
		} catch(err) {
			console.error(App.Util.error("PING KO - " + err.message, 3000));
		}
	}

	ping1() { this.ping(this.$.spin); }
	ping2() { this.ping(App.globalSpinner); }
	ping3() { this.ping(App.defaultSpinner); }

	async dbInfo() {
		try {
			const r = await new Req().setOp("dbInfo").setTimeOut(6000).go();
			this.pingres = !r ? "json mal formé ?" : JSON.stringify(r);					
		} catch(err) {
			console.error(App.Util.error("DbInfo KO - " + err.message, 3000));
		}
	}
		
	async crypto() {
		try{
			await App.TestCrypto.test();
		} catch(err) {
			App.scriptErr(err);
		}
	}
	
	// 	constructor(op, code, phase, message, detail) {
	erA() {
		App.globalReqErr.open(this, new App.ReqErr("opr", "A_T", 0, "message de A", [this.errTest.message, this.errTest.stack]));
	}
	erB() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "B_T", 1, "message de B", [this.errTest.message, this.errTest.stack]));
	}
	erX() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "X_T", 2, "message de X", [this.errTest.message, this.errTest.stack]));
	}
	erD() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "DBUILD", 0, "message de D", [this.errTest.message, this.errTest.stack]));
	}
	erC() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "CONT", 2, "message de C", [this.errTest.message, this.errTest.stack]));
	}
	erO() {
		App.globalReqErr.open(this, new App.ReqErr("opr", "OFF", 0, "message de off", [this.errTest.message, this.errTest.stack]));
	}
	erS() {
		App.globalReqErr.open(this, new App.ReqErr("opr", "SAUTH", 1, "pas autorisé", [this.errTest.message, this.errTest.stack]));
	}
	erT() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "TIMEOUT", 3, "timeout 32s", [this.errTest.message, this.errTest.stack]));
	}
	erI() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "INTERRUPT", 9, "interruption par clic", [this.errTest.message, this.errTest.stack]));
	}
	erL() {
		App.globalReqErr.open(this, new App.ReqErr("opw", "LREC", 6, "parse json", [this.errTest.message, this.errTest.stack]));
	}
	erS0() {
		App.scriptErr(this.errTest);
	}
	erS1() {
		App.scriptErr(this.errTest, true);
	}
	
	handleTrack(e) {
		let obj = e.detail.hover();
		switch(e.detail.state) {
		case 'start':
			this.dragSrcEl = e.target;
			this.dragHover = this.dragSrcEl;
			this.dragStart(e.detail.x, e.detail.y);
            break;
		case 'track':
			this.dragMove(e.detail.x, e.detail.y);
			if (obj && obj != this.dragHover) {
				if (this.dragHover) this.dragLeave(this.dragHover);
				if (obj.hasAttribute("drop-target")) {
					this.dragHover = obj;
					this.dragEnter(this.dragHover);
				} else 
					this.dragHover = null;
			}
            break;
		case 'end':
			if (obj && this.dragHover && obj != this.dragHover) this.dragLeave(this.dragHover);
			this.dragEnd(obj);
            break;
        }
	}
		
	dragStart(x, y) {
		this.dragSrcEl.classList.add('over');
		this.rect = this.dragSrcEl.getBoundingClientRect();
		let g = this.$.ghost;
		g.innerHTML = this.dragSrcEl.id;
		g.style.display = "block";
		this.dragMove(x, y);
		console.log("Tracking started on " + (this.dragSrcEl.id ? this.dragSrcEl.id : "?"));
	}

	dragMove(x, y) {
		let g = this.$.ghost;
		g.style.top = "" + (y - (this.rect.height / 2)) + "px";
		g.style.left = "" + (x + 30) + "px";
	}

	dragEnter(obj) {
		obj.classList.add('over');
		console.log("Enter on " + (obj.id ? obj.id : "?"));
	}

	dragLeave(obj) {
		obj.classList.remove('over');
		console.log("Leave on " + (obj.id ? obj.id : "?"));
	}

	dragEnd(obj) {
		let g = this.$.ghost;
		g.style.display = "none";
		if (obj && obj.hasAttribute("drop-target")) {
			obj.classList.remove('over');
			console.log("Drop on " + (obj.id ? obj.id : "?"));	
            if (this.dragSrcEl != obj) {
            	let x = this.dragSrcEl.firstChild.innerHTML;
                this.dragSrcEl.firstChild.innerHTML = obj.firstChild.innerHTML;
                obj.firstChild.innerHTML = x;
            }
		} else {
			console.log("Cancel drag");						
		}
	}
	
//    createImage(e) {
//        // just in case...
//        if (this._img) {
//            this._destroyImage();
//        }
//        // create drag image from custom element or drag source
//        var src = this._imgCustom || this._dragSource;
//        this._img = src.cloneNode(true);
//        this._copyStyle(src, this._img);
//        this._img.style.top = this._img.style.left = '-9999px';
//        // if creating from drag source, apply offset and opacity
//        if (!this._imgCustom) {
//            var rc = src.getBoundingClientRect(), pt = this._getPoint(e);
//            this._imgOffset = { x: pt.x - rc.left, y: pt.y - rc.top };
//            this._img.style.opacity = DragDropTouch._OPACITY.toString();
//        }
//        // add image to document
//        this._moveImage(e);
//        document.body.appendChild(this._img);
//    }
//    
//    destroyImage() {
//        if (this._img && this._img.parentElement) {
//            this._img.parentElement.removeChild(this._img);
//        }
//        this._img = null;
//        this._imgCustom = null;
//    }
//
//    moveImage(e) {
//        var _this = this;
//        requestAnimationFrame(function () {
//        	if (_this._img) {
//	            var pt = _this._getPoint(e, true), s = _this._img.style;
//	            s.position = 'absolute';
//	            s.pointerEvents = 'none';
//	            s.zIndex = '999999';
//	            s.left = Math.round(pt.x - _this._imgOffset.x) + 'px';
//	            s.top = Math.round(pt.y - _this._imgOffset.y) + 'px';
//        	}
//        });
//    };
//
//    handleDragStart(e) {
//        if (e.target.className.indexOf('column') > -1) {
//            this.dragSrcEl = e.target;
//            this.dragSrcEl.style.opacity = '0.4';
//            let dt = e.dataTransfer;
//            dt.effectAllowed = 'move';
//            dt.setData('text', this.dragSrcEl.innerHTML);
//
//            var el = this.getDDTarget();
//            
//            console.log(e.target.id + " start");
//
//            // customize drag image for one of the panels
//            if (dt.setDragImage instanceof Function && e.target.innerHTML.indexOf('X') > -1) {
//                var img = new Image();
//                img.src = App.superman;
//                dt.setDragImage(img, img.width, img.height);
//            }
//        }
//    }
//    handleDragOver(e) {
//        if (this.dragSrcEl) {
//            e.preventDefault();
//            e.dataTransfer.dropEffect = 'move';
//            console.log(e.target.id + " over");
//        }
//    }
//    handleDragEnter(e) {
//        if (this.dragSrcEl) {
//            e.target.classList.add('over');
//            console.log(e.target.id + " enter");
//        }
//    }
//    handleDragLeave(e) {
//        if (this.dragSrcEl) {
//            e.target.classList.remove('over');
//            console.log(e.target.id + " leave");
//        }
//    }
//    handleDragEnd(e) {
//        this.dragSrcEl = null;
//        console.log(e.target.id + " end");
//        [].forEach.call(this.cols, function (col) {
//            col.style.opacity = '';
//            col.classList.remove('over');
//        });
//    }
//    handleDrop(e) {
//        if (this.dragSrcEl) {
//            console.log(e.target.id + " drop");
//            e.stopPropagation();
//            e.stopImmediatePropagation();
//            e.preventDefault();
//            if (this.dragSrcEl != e.target) {
//                this.dragSrcEl.innerHTML = e.target.innerHTML;
//                e.target.innerHTML = e.dataTransfer.getData('text');
//            }
//        }
//    }

}
customElements.define(Home2Home.is, Home2Home);
