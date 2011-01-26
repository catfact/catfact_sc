/*
Document.current.background_(Color.new(0.5, 0.5, 0.5, 1.0));
*/

// an animation compositor
CfCanvas {
	var <>window;		// SCWindow
	var <>uv;			// SCUSerView
	var <>layers;		// list of CfAnimationLayer
	var <>routine; 	// Routine
	var <>dt;			// time between redraws

	*new { 
		arg name;
		^super.new.cfCanvasInit(name);	
	}
	
	cfCanvasInit {
		arg argName;
		window = SCWindow(argName, SCWindow.screenBounds, border:false).front;
		window.view.background_(Color.new(0, 0, 0, 0));
		window.view.keyDownAction = { arg v, c, m, u, k;
			if(c == $c, { window.close; });
		};
		layers = List.new;
		uv = SCUserView(window.view, window.view.bounds);
		uv.background_(Color.new(0, 0, 0, 0));
		uv.keyDownAction = {
			arg v, c, m, u, k;
			if (c == $f, {
				window.fullScreen;
			});
			
			if (c == $c, {
				window.endFullScreen.close;
			});
		};
		
		CmdPeriod.doOnce {Êwindow.close };
		
		uv.drawFunc = {
			layers.do({ arg layer;
				layer.draw;
			});
		};
		
		dt = 1 / 25;
		
		routine = Routine {inf.do{
			{ window.refresh; }.defer;
			dt.wait;
		}};
	}
	
	play {
		routine.play;
	}
	
	stop {
		routine.stop;
	}
	
	reset {
		routine.reset; 
	}
}

// like CfCanvas, with more specific functionality: morphs between colors and update rates
// so, a dedicated hue layer is maintained
CfHueMorphCanvas : CfCanvas {
	var <>hueLayer;		// CfColorPane
	var <>color;			// current Color
	var <>targetH;		// target Color
	var <>blendH;			// linear index in [0, 1]
	var <>blendFactorH;	// blend ratio per step (exponential)
	var <>slopeDt;		// interpolate between update rates as well
	var <>targetDt;
	
	*new { arg name;
		^super.new.cfHueMorphCanvasInit(name);
	}
	
	cfHueMorphCanvasInit {			
		//uv.postln;
		//layers.postln;
		
		hueLayer = CfColorPane(uv.bounds);
		targetH = Color.new(0, 0, 0);
		color = Color.grey;
		blendFactorH = 0.0;		
		targetDt = dt;
		slopeDt = 0.0;
		
		uv.drawFunc = {
			if (blendFactorH > 0.0, {
				color = color.hueBlend(targetH, blendFactorH);
				if (color == targetH, { blendFactorH = 0.0; });
				hueLayer.color_(color);
			});
			
			if (dt != targetDt, {
				dt = dt + slopeDt;
			});
			if (abs(dt - targetDt) < abs(slopeDt), { dt = targetDt; });
			
			hueLayer.draw;
			layers.do({ arg layer;
				layer.draw;
			});
		};
	}
	
	setColor { arg argColor; // in RGB
		color = argColor;
		hueLayer.color = color;
	}
	
	setTargetH { arg argColor; // in RGB
		targetH = argColor;
	}
	
	setBlendFactorH { arg factor;
		blendFactorH = factor;
	}
	
	setTargetDt { arg target, numfades;
		targetDt = target;
		slopeDt = (target - dt) / numfades;
	}
}

// animation layer; executes an arbitrary draw function. 
CfAnimationLayer {
	var <>draw_f;
	var <>alpha, <>mode;
	
	*new {
		^super.new.cfAnimationLayerInit;
	}
	
	cfAnimationLayerInit {
		alpha = 1.0;
		mode = 1;
	}
	
	draw {
		Pen.alpha_(alpha);
		Pen.blendMode_(mode);
		Pen.beginTransparencyLayer;
		draw_f.value;
		Pen.endTransparencyLayer;
	}
}

// animation layer; draws a solid pane of color
CfColorPane : CfAnimationLayer {
	var <>color, <>bounds;
	
	*new { arg bounds;
		^super.new.cfPanelAnimationLayerInit(bounds);
	}
	
	cfPanelAnimationLayerInit {
		arg argBounds;
		bounds = argBounds;
		color = Color.clear;
	}
	
	draw {
		Pen.alpha_(alpha);
		Pen.blendMode_(mode);
		Pen.beginTransparencyLayer;

		Pen.addRect(bounds);
		Pen.fillColor_(color);
		Pen.fill;

		Pen.endTransparencyLayer;
	}
}

// runs a 2d model and traces its history
// draws a simple scrolling grid
CfGridTrace2d : CfAnimationLayer {
	var <>model, <>bounds;
	var <>n;
	var <>history, <>h;
	var <>blockW, <>blockH;
	
	*new { arg bounds, n, h;
		^super.new.cfGridTrace2dInit(bounds, n, h);
	}
	
	cfGridTrace2dInit {
		arg argBounds, argN, argH;
		bounds = argBounds;
		n = argN;
		model = CfModel2d.new(n);
		h = argH;
		history = Array.fill(h, { Array.fill(model.n, {0.0}); });
		blockW = bounds.width / n;
		blockH = bounds.height / h;
	}
	
	draw {
		model.iter;
		history = history.copyRange(1, h-1);
		history = history.add(Array.newFrom(model.val));
		
		Pen.alpha_(alpha);
		Pen.blendMode_(mode);
		Pen.beginTransparencyLayer;

		history.do({ arg valArr, j;
			valArr.do({ arg val, i; 
				if (val > 0, {
					Pen.fillColor = Color.new(val, val, val);
				}, {
					Pen.fillColor = Color.new(val * -1, val * -0.5, val * -0.5);
				});
				Pen.fillRect(Rect(i * blockW - 1,
					j * blockH - 1,
					blockW + 2,
					blockH + 2
				));
			});
		});	

		Pen.endTransparencyLayer;
	}	
}

// runs a 2d model and traces its history
// draws a spiral of squares...
/*
CfTriSpiralTrace2d : CfAnimationLayer {
	var <>model, <>bounds;
	var <>n;
	var <>history, <>h;
	var <>phi, <>dphi;
	var <>dr;
	var <>iScale, <>jScale;
	var <>offset;
	
	*new { arg bounds, n, h;
		^super.new.cfTriSpiralTrace2dInit(bounds, n, h);
	}
	
	cfTriSpiralTrace2dInit {
		arg argBounds, argN, argH;
		bounds = argBounds;
		n = argN;
		model = CfModel2d.new(n);
		h = argH;
		history = Array.fill(h, { Array.fill(model.n, {0.0}); });
		dphi = pi / h;
		phi = 0.0;
		dr = 10;
		iScale = 3;
		jScale = 5;
		offset = 10;
	}
	
	draw {
		var l;
		model.iter;
		history = history.copyRange(1, h-1);
		history = history.add(Array.newFrom(model.val));
		
		Pen.alpha_(alpha);
		Pen.blendMode_(mode);
		Pen.beginTransparencyLayer;

		Pen.translate(bounds.width * 0.5, bounds.height * 0.5);
		
		history.do({ arg valArr, j;
			valArr.do({ arg val, i; 
				//l = i*iScale + j*jScale + offset;
				l = 100 + i + j;
				if (val > 0, {
					Pen.fillColor = Color.new(val, val, val);
				}, {
					Pen.fillColor = Color.new(val * -1, val * -0.5, val * -0.5);
				});
				Pen.rotate(dphi);
				//phi = phi + dphi;
				// Pen.translate(cos(phi) * dr, sin(phi) * dr)
				Pen.fillRect(Rect(0, 0, l, l));
			});
		});	

		Pen.endTransparencyLayer;
	}	
}
*/

// trace a circular grid waterfall
CfWedgeTrace2d : CfAnimationLayer {
	var <>model, <>bounds;
	var <>n;
	var <>history, <>h;
	var <>phi, <>dphi;
	var <>radius;
	var <>rScale;
	
	*new { arg bounds, n, h;
		^super.new.cfWedgeTrace2dInit(bounds, n, h);
	}
	
	cfWedgeTrace2dInit { arg argBounds, argN, argH;
		bounds = argBounds;
		n = argN;
		model = CfModel2d.new(n);
		h = argH;
		history = Array.fill(h, { Array.fill(model.n, {0.0}); });
		dphi = 2pi / n;
		phi = 0.0;
		radius = 10;
	}
	
	draw {
		var l;
		model.iter;
		history = history.copyRange(1, h-1);
		history = history.add(Array.newFrom(model.val));
		
		Pen.alpha_(alpha);
		Pen.blendMode_(mode);
		Pen.beginTransparencyLayer;

		Pen.translate(bounds.width * 0.5, bounds.height * 0.5);
		
		radius.postln;
		
		history.do({ arg valArr, j;
			valArr.do({ arg val, i;
				if (val > 0, {
					Pen.color = Color.new(val, val, val);
				}, {
					Pen.color = Color.new(val * -1, val * -0.5, val * -0.5);
				});
				Pen.color = Color.rand;
				//Pen.beginPath;
				Pen.addWedge(
					Point(0, 0), //bounds.width*0.5, bounds.height*0.5),
					//400.rand, // j * 40, //(j+1)*dr,
					(h-j)*radius + radius,
					//2pi / 16 * i, 
					dphi * i,
					//2pi / 16 
					dphi
				);
				Pen.perform(\fill);
				//Pen.rotate(dphi);
				//phi = phi + dphi;
				// Pen.translate(cos(phi) * dr, sin(phi) * dr)
				//Pen.fillRect(Rect(0, 0, l, l));
				
			});
		});	

		Pen.endTransparencyLayer;
	}	
}

// trace with weird shards
CfShardTrace2d : CfAnimationLayer {
	var <>model, <>bounds;
	var <>n;
	var <>history, <>h;
	var <>phi, <>dphi;
	var <>dr;
	var <>rScale;
	
	*new { arg bounds, n, h;
		^super.new.cfShardTrace2dInit(bounds, n, h);
	}
	
	cfShardTrace2dInit {
		arg argBounds, argN, argH;
		bounds = argBounds;
		n = argN;
		model = CfModel2d.new(n);
		h = argH;
		history = Array.fill(h, { Array.fill(model.n, {0.0}); });
		dphi = 2pi / h;
		phi = 0.0;
		dr = 50;
	}
	
	draw {
		var l;
		model.iter;
		history = history.copyRange(1, h-1);
		history = history.add(Array.newFrom(model.val));
		
		Pen.alpha_(alpha);
		Pen.blendMode_(mode);
		Pen.beginTransparencyLayer;

		Pen.translate(bounds.width * 0.5, bounds.height * 0.5);
		
		history.do({ arg valArr, j;
			valArr.do({ arg val, i;
				if (val > 0, {
					Pen.fillColor = Color.new(val, val, val);
				}, {
					Pen.fillColor = Color.new(val * -1, val * -0.5, val * -0.5);
				});
				Pen.beginPath;
				Pen.addAnnularWedge(
					Point(0, 0), //bounds.width*0.5, bounds.height*0.5),
					j * dr,
					(j+1)*dr,
					dphi * i,
					dphi
				);
				Pen.fill;
				//Pen.rotate(dphi);
				//phi = phi + dphi;
				// Pen.translate(cos(phi) * dr, sin(phi) * dr)
				//Pen.fillRect(Rect(0, 0, l, l));
				
			});
		});	

		Pen.endTransparencyLayer;
	}	
}