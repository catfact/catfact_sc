/*

CfTapDelay {
	var <>delay0, <>delay1,	// 2 delay synths
		<>whichdelay,		// a toggle
		<>tapper;			// a timer
		
	*new {
		^super.new.init;
	}
	
	init {
			
		SynthDef.new(\bufdb, {arg in=0, out=0, buf=0, time=1.0, amp=1.0, fbamp=0.0, inamp=1.0, amplag=1.0;
			var input = In.ar(in) + (LocalIn.ar(1) * fbamp);
			var delay;
			amp = Lag.kr(amp, amplag);
			delay = BufDelayC.ar(buf, input*inamp, time);
			LocalOut.ar(delay);
			Out.ar(out, delay * amp);
		}).send(s);

	}
	
}
*/