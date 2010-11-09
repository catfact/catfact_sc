PrimeSineSpiral {
	var <>s, <>out=0;
	var <>factorList;
	var <>wrapRatio=4.0, <>hz0=120;
	var <>atkScale=0.0, <>atkOff=0.005, <>sustain=0.0, <>relScale=0.0, <>relOff=1.0;
	var <>amp=0.5, <>wait=0.25; 
	var <>waitScale=0.0;
	var <>playR;
	
	*new { arg server;
		^super.new.init(server);
	}
	
	init { arg server;
		s = server;
		factorList = List[ 3 ];
	}
	
	geoWrap {
		arg x, wrapFactor;
		var wrapped, wrapRecurse;
		wrapped = 1.0;
		wrapRecurse = { arg v;
	        if (v < wrapFactor, {
	            wrapped = v;
	        }, {
	            if ( v < 1.0, {
	                v = v * wrapFactor;
	                wrapRecurse.value(v);
	            }, {
	                v = v / wrapFactor;
	                wrapRecurse.value(v);
	            });
	        });
	    };
	    wrapRecurse.value(x);    
	    ^wrapped
	}
	
	play { arg n;
		var ratio = 1.0, idx = -1;
		playR = Routine { n.do({ 
			var atk, sus, rel;
			idx = (idx + 1) % (factorList.size);
			ratio = ratio *  factorList[idx];
			ratio = this.geoWrap(ratio, wrapRatio);
			sus = sustain;
			atk = (atkScale / ratio) * atkScale + atkOff;
	        	rel = (ratio - 1.0) * relScale + relOff;
	        	{
				var env;
				var envgen;
				env = Env.new([0.0, 1.0, 1.0, 0.0], [atk, sus, rel]);
				envgen = EnvGen.ar(env, gate:1, doneAction:2);ß
				Out.ar(out, Pan2.ar(
					SinOsc.ar(ratio * hz0) * envgen * amp,
					(ratio * 16.0) % 2.0 - 1.0 
				));
			}.play(s);
			if (waitScale > 0, {
				(wait * (1.0-waitScale) + (wait / ratio * waitScale)).wait;
			}, {
				(wait * (1.0-waitScale) + (wait * ratio * waitScale)).wait;
			});
		}); }.play;
	}	
}