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
	var <>blendH, <>targetH, blendFactorH;
	var <>hueLayer;
	var <>color;
	
	*new { arg name;
		^super.new.cfMorphCanvasInit(name);
	}
	
	cfMorphCanvasInit {			
		uv.postln;
		layers.postln;
		
		hueLayer = CfPanelAnimationLayer(uv.bounds);
		targetH = Color.new(0, 0, 0);
		color = Color.grey;
		blendFactorH = 0.0;		
		
		uv.drawFunc = {
			if (blendFactorH > 0.0, {
				color = color.hueBlend(targetH, blendFactorH);
				if (color == targetH, { blendFactorH = 0.0; });
				hueLayer.color_(color);
			});
			
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
CfPanelAnimationLayer : CfAnimationLayer {
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

// animation layer; nonlinear spring torus
CfFpuRingAnimationLayer : CfAnimationLayer {
	var <>model, <>bounds;
	
	*new { arg bounds;
		^super.new.cfFpuRingAnimationLayerInit(bounds);
	}
	
	cfFpuRingAnimationLayerInit {
		arg argBounds;
		bounds = argBounds;
		
		model = CfFpuRing.new;
	}
	
	draw {
		model.iter;
		Pen.alpha_(alpha);
		Pen.blendMode_(mode);
		Pen.beginTransparencyLayer;

		model.val.do({ arg pos, i;
			if (pos > 0, {
				Pen.fillColor = Color.new(pos, pos, pos);
			}, {
				Pen.fillColor = Color.new(pos * -1, pos * -0.5, pos * -0.5);
			});
			Pen.fillRect(Rect(i * (bounds.width/model.n) - 1, 0, bounds.width/model.n + 2, bounds.height));
		});

		Pen.endTransparencyLayer;
	}	
}

// animation layer; nonlinear spring torus with history
CfFpuRingTraceAnimationLayer : CfAnimationLayer {
	var <>model, <>bounds;
	var <>history, <>h;
	var <>blockW, <>blockH;
	
	*new { arg bounds, n, h;
		^super.new.cfFpuRingTraceAnimationLayerInit(bounds, n, h);
	}
	
	cfFpuRingTraceAnimationLayerInit {
		arg argBounds, argN, argH;
		bounds = argBounds;
		
		model = CfFpuRing.new(argN);
		h = argH;
		history = Array.fill(h, { Array.fill(model.n, {0.0}); });
		blockW = bounds.width / argN;
		blockH = bounds.height / h;
	}
	
	draw {
		model.iter;
		history = history.copyRange(1, h-1);
		history = history.add(Array.newFrom(model.pos));
		
		Pen.alpha_(alpha);
		Pen.blendMode_(mode);
		Pen.beginTransparencyLayer;

		history.do({ arg posArr, j;
			posArr.do({ arg pos, i; 
				if (pos > 0, {
					Pen.fillColor = Color.new(pos, pos, pos);
				}, {
					Pen.fillColor = Color.new(pos * -1, pos * -0.5, pos * -0.5);
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