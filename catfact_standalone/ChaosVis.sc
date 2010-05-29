DorbandVisualizer {
	// the "<" and ">" tell SC to manufacture getter/setter methods respectively
	// so you could modify the state and parameter of the class after it's created
	var <window, <view, <width, <height;
	var <>colors;
	var <dist = -1;	// distance from previous point
	var <>point; 		// dorband state (a Point object in [0, 1]) 
	var <>c; 			// dorband parameter
	
	// arbitrary scale for sonification
	var pitches;
	
	*new { // default arguments are here
		arg width=400, height=400, x0=0.3, y0=0.8, c=0.9;
		^super.new.init(width, height, x0, y0, c);
	}
	
	init {
	
		//--------------------------------------------------------
		// graphics, after expo16.js
	
		arg widthArg, heightArg, x0, y0, cArg; 
		width = widthArg;
		height = heightArg;
		
		point = Point.new(x0, y0);
		c = cArg;
		
		point.postln;
		c.postln;
		
		window = Window.new("dorband", Rect(0, 0, width, height));
		window.view.background = Color.black;
		window.front;
		
		// make a subview to do the actual drawing
		// (using a slightly more flexible graphics class)
		view = UserView(window, window.bounds).clearOnRefresh_(false);
		view.background = Color.black;
		
		// here's the graphics update function
		view.drawFunc = { 
			if (dist > -1, { 	// dumb check for the first draw
				var rect;
				rect = Rect.new(
					view.bounds.width * point.x,
					view.bounds.height * point.y,
					2,
					2
				);
				Pen.addRect(rect);
				Pen.color = colors.blendAt(dist * 10);
				Pen.perform(\fill);
			});
		};
		
		// let's use spacebar instead of a mouse click,
		// easier on the carpal tunnel
		window.view.keyDownAction = {
			arg view, char;
			if (char == $ , {
				this.click;
			});
		};
		
		colors = Array.with(
			Color.red,
			Color.new255(255, 128, 0),
			Color.yellow,
			Color.new255(128, 255, 0),
			Color.green,
			Color.new255(0, 255, 128),
			Color.cyan,
			Color.new255(0, 128, 255),
			Color.blue,
			Color.new255(128, 0, 255),
			Color.magenta,
			Color.new255(255, 0, 128),
			Color.white,	// overscale
			Color.white,
			Color.white
		);			
		
		
		///-----------------
		// some very weird just-intonation scale for sonification
		pitches = Array.fill(36, {arg i;
			(((i*2) % 10) + 2) / ((i%12) + 1) * (2.0 ** (i/12).floor) * 220.0;
		});
		// remove duplicates
		pitches = pitches.asSet.asArray.sort;
		pitches.postln;
	}
	
	dorband {
		var newPoint = Point.new;
		newPoint.x = ((1.0 - c) * point.x + (4.0 * c * point.y) * (1.0 - point.y));
		newPoint.y = ((1.0 - c) * point.y + (4.0 * c * point.x) * (1.0 - point.x));
		^newPoint;
	}
	
	click {
		var newPoint;
		// "click".postln;
		newPoint = this.dorband();
		("new point: " ++ newPoint).postln;
		dist = point.dist(newPoint);
		("distance: " ++ dist).postln;
		".".postln;
		point = newPoint;
		view.refresh;
		
		//--------------------------------
		// audio
				
		// make a little additive synth note
		play({
			// timbre from distance
			var harmonics = (dist * 5).floor.max(1);
			// duration from y
			var dur = 0.25 + (point.y*4.0);
			// pitch from x
			var hz = pitches.foldAt(point.x * pitches.size);
			// array of even harmonics with some decay
			Pan2.ar(Klang.ar(`[
				Array.series(harmonics, 1, 2),
				Array.geom(harmonics, 1.0, -6.dbamp),
				Array.rand(harmonics, 0.0, pi)
			], hz, 0)) // multiply by a percussive envelope
				* EnvGen.ar(Env.perc(0.01, dur), 1.0, doneAction: 2) * 0.05;
		});

	}
	
}