Tog {
	
	var win;
	
	var <>keycodes, <>chars;
	var <>keysdown;
	var <>synths;
	
	var <>freqs, <>numfreqs, <>basefreq;
	
	var <>amp = 0.02;
	
	var <>s;
	
	*new {
		^super.new.init;	
	}	
	init {		
		s = Server.default;
		s.waitForBoot { 
		
		keysdown = Dictionary.new;
		
		SynthDef.new(\sine, {arg out=0, amp=0.0, amplag=1.0, hz=220.0;
			
			Out.ar(out, Pan2.ar(Lag.kr(amp, amplag) * SinOsc.ar(hz), 0));
		}).store;
		
		/*
		x = Synth.new(\sine, [\hz, 220]);	
		x.set(\amp, 1.0);
		x.set(\amp, 0.0);
		*/
		
		keycodes = List[ 49, 113, 50, 119, 51, 101, 52, 114, 53, 116, 54, 121, 55, 117, 56, 105, 57, 111, 48, 112, 45, 91, 61, 93, 97, 122, 115, 120, 100, 99, 102, 118, 103, 98, 104, 110, 106, 109, 107, 44, 108, 46, 59, 47, 39 ];
		
		chars = List[$1, $q, $2, $w, $3, $e, $4, $r, $5, $t, $6, $y, $7, $u, $8, $i, $9, $o, $0, $p, $-, $[, $=, $], $a, $z, $s, $x, $d, $c, $f, $v, $g, $b, $h, $n, $j, $m, $k, $,, $l, $., $;, $/, $' ];
	
		freqs = [1, 13/12, 9/8, 6/5, 5/4, 4/3, 45/32, 3/2, 8/5, 10/6, 16/9, 24/13];
		numfreqs = freqs.size;
		basefreq = 220;
		
		synths = Dictionary.new;
		
		keycodes.do({ arg code, i;
			synths.add(
				code -> Synth.new(\sine, [
					\out, 0,
					\amp, 0.0,
					\amplag, 4.0,
					\hz, freqs[i % (numfreqs)] * (2 ** ((i / numfreqs).floor)) * basefreq;
				]);
			);
		});
		
		win = SCWindow.new;
		
		win.view.keyDownAction = ({ arg view, char, unicode, keycode; 
			//keycodes.add(keycode); keycodes.postln;
			if (keycodes.includes(keycode), {
				synths.at(keycode).set(\amp, amp);
				synths.at(keycode).postln;
			});
		});
		
		win.view.keyUpAction = ({ arg view, char, unicode, keycode; 
			if (keycodes.includes(keycode), {
				synths.at(keycode).set(\amp, 0.0);
				synths.at(keycode).postln;
			});
		});
		
		win.front;
		} // /waitforboot
	} // /init
	
	tune { arg base;
		basefreq = base;
		keycodes.do({ arg code, i; synths.at(code).set(
			\hz, freqs[i % (numfreqs)] * (2 ** ((i / numfreqs).floor)) * basefreq;
		); });
	}
} // /Keysine