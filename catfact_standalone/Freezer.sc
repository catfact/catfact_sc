//========================================
// more optimized with PV_Copy, single input analysis
/*
Freezer8 {
	var <numfreeze;
	var <>bufs, <>tappers, <>voices, <>voiceflags;
	var <server;
	var <>level=0.5;
	var <panspread=0.2;
	var <>ratiocomponents;
	var <>stretchtappers;
	
	*new {
		arg server, in, out, target, addAction;
		^super.new.init(server, in, out, target, addAction);
	}
	
	init { arg serverArg, in=0, out=0, target=Server.default, addAction=\addToTail;

	Routine {
		server = serverArg;
		
		numfreeze = 9;
		bufs = Array.fill(numfreeze, {
			//Buffer.alloc(server, 16384, 1);
			Buffer.alloc(server, 8192, 1);
		});
		
		0.1.wait;
		
		tappers = Array.fill(numfreeze, { CfTapper.new; });
		tappers.do({|t|t.tap;});
		

		stretchtappers = Array.fill(numfreeze, { CfTapper.new; });
		stretchtappers.do({|t|t.tap;});
		
		
		SynthDef.new(\specfreeze_shift, {
			arg  out=0, in=0, buf=0, freeze=0,
				amp=1.0, amplag=0.1,
				stretch=1.0, shift=1.0,
				stretchlag=0.1, shiftlag=0.1,
				pan=0.0;
			var chain, input, output;
			stretch = Lag.kr(stretch, stretchlag);
			shift = Lag.kr(shift, shiftlag);
			input = In.ar(in);
			chain = FFT(buf, input);
			chain = PV_Freeze(chain, freeze);
			chain = PV_BinShift(chain, stretch, shift);
			output = IFFT(chain);
			amp = Lag.kr(amp, amplag);
			Out.ar(out, Pan2.ar(output  * amp, pan));
		}).store;
		
		0.1.wait;
		
		voiceflags = Array.fill(numfreeze, {false});
		
		ratiocomponents = Array.fill(6, {1.0});
		
		voices = Array.fill(numfreeze, { arg i;
			var thepan;
			thepan = (i / numfreeze) * panspread - (panspread * 0.5);
			thepan.postln;
			Synth.new(\specfreeze_shift,[
				\in, in,
				\out, out,
				\buf, bufs[i],
				\amp, 0.0,
				\pan, thepan
			], target, addAction);
		});
		
	}.play }
	
	start_note { arg which;
		tappers[which].tap;
		voices[which].set(\freeze, 0);

	}
	
	continue_starting { arg which;
		var dur = tappers[which].tap;
		voices[which].set(\amplag, dur);
		voices[which].set(\amp, level);
		voiceflags[which] = true;
		SystemClock.sched(0.1, {voices[which].set(\freeze, 1); nil});
	}
	
	stop_note { arg which;
		tappers[which].tap;
	}

	continue_stopping { arg which;
		var dur = tappers[which].tap;
		voices[which].set(\amplag, dur);
		voices[which].set(\amp, 0.0);
		voiceflags[which] = false;
	}
	
	setRatioComponent {arg which, val;
		ratiocomponents[which] = val;
		if (which > 2, { // x
			var x = (which - 3);
			3.do({arg y;
				post(x);
				post(y);
				postln(val * ratiocomponents[y]);
				//voices[x*3 + y].set(\stretchlag, stretchtappers[x*3+y].tap);
				voices[x*3 + y].set(\stretch, val * ratiocomponents[y];);
			});
		}, { // y
			var y = which;
			3.do({arg x;
				post(x);
				post(y);
				postln(val * ratiocomponents[x+3]);
				//voices[x*3 + y].set(\stretchlag, stretchtappers[x*3+y].tap);
				voices[x*3 + y].set(\stretch, val * ratiocomponents[x+3];);
			});
		});
	}
	
	kill {
		voices.do({ arg synth; synth.free; });
		bufs.do({ arg buf; buf.free; });
	}
	
} // Freezer8
*/


// ============================ earlier, unoptimized

Freezer9 {
	
	var <numfreeze;
	var <>bufs, <>tappers, <>voices, <>voiceflags;
	var <server;
	var <>level=0.5;
	var <panspread=0.2;
	var <>ratiocomponents;
	var <>stretchtappers;
	
	*new {
		arg server, in, out, target, addAction;
		^super.new.init(server, in, out, target, addAction);
	}
	
	init { arg serverArg, in=0, out=0, target=Server.default, addAction=\addToTail;

	Routine {
		server = serverArg;
		
		numfreeze = 9;
		bufs = Array.fill(numfreeze, {
			//Buffer.alloc(server, 16384, 1);
			Buffer.alloc(server, 8192, 1);
		});
		
		0.1.wait;
		
		tappers = Array.fill(numfreeze, { CfTapper.new; });
		tappers.do({|t|t.tap;});
		

		stretchtappers = Array.fill(numfreeze, { CfTapper.new; });
		stretchtappers.do({|t|t.tap;});
		
		
		SynthDef.new(\specfreeze_shift, {
			arg  out=0, in=0, buf=0, freeze=0,
				amp=1.0, amplag=0.1,
				stretch=1.0, shift=1.0,
				stretchlag=0.1, shiftlag=0.1,
				pan=0.0;
			var chain, input, output;
			stretch = Lag.kr(stretch, stretchlag);
			shift = Lag.kr(shift, shiftlag);
			input = In.ar(in);
			chain = FFT(buf, input);
			chain = PV_Freeze(chain, freeze);
			chain = PV_BinShift(chain, stretch, shift);
			output = IFFT(chain);
			amp = Lag.kr(amp, amplag);
			Out.ar(out, Pan2.ar(output  * amp, pan));
		}).store;
		
		0.1.wait;
		
		voiceflags = Array.fill(numfreeze, {false});
		
		ratiocomponents = Array.fill(6, {1.0});
		
		voices = Array.fill(numfreeze, { arg i;
			var thepan;
			thepan = (i / numfreeze) * panspread - (panspread * 0.5);
			thepan.postln;
			Synth.new(\specfreeze_shift,[
				\in, in,
				\out, out,
				\buf, bufs[i],
				\amp, 0.0,
				\pan, thepan
			], target, addAction);
		});
		
	}.play }
	
	start_note { arg which;
		tappers[which].tap;
		voices[which].set(\freeze, 0);

	}
	
	continue_starting { arg which;
		var dur = tappers[which].tap;
		voices[which].set(\amplag, dur);
		voices[which].set(\amp, level);
		voiceflags[which] = true;
		SystemClock.sched(0.1, {voices[which].set(\freeze, 1); nil});
	}
	
	stop_note { arg which;
		tappers[which].tap;
	}

	continue_stopping { arg which;
		var dur = tappers[which].tap;
		voices[which].set(\amplag, dur);
		voices[which].set(\amp, 0.0);
		voiceflags[which] = false;
	}
	
	setRatioComponent {arg which, val;
		ratiocomponents[which] = val;
		if (which > 2, { // x
			var x = (which - 3);
			3.do({arg y;
				post(x);
				post(y);
				postln(val * ratiocomponents[y]);
				//voices[x*3 + y].set(\stretchlag, stretchtappers[x*3+y].tap);
				voices[x*3 + y].set(\stretch, val * ratiocomponents[y];);
			});
		}, { // y
			var y = which;
			3.do({arg x;
				post(x);
				post(y);
				postln(val * ratiocomponents[x+3]);
				//voices[x*3 + y].set(\stretchlag, stretchtappers[x*3+y].tap);
				voices[x*3 + y].set(\stretch, val * ratiocomponents[x+3];);
			});
		});
	}
	
	kill {
		voices.do({ arg synth; synth.free; });
		bufs.do({ arg buf; buf.free; });
	}
	
} // Freezer9